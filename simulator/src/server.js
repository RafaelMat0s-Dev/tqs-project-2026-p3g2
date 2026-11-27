import http from "node:http";
import { toReading } from "./reading.js";

const JSON_HEADERS = { "content-type": "application/json" };

function send(response, status, body) {
  response.writeHead(status, JSON_HEADERS);
  response.end(JSON.stringify(body));
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    let data = "";
    request.on("data", (chunk) => (data += chunk));
    request.on("end", () => resolve(data));
    request.on("error", reject);
  });
}

function validate(scenario, fleet) {
  if (scenario === null || typeof scenario !== "object" || Array.isArray(scenario)) return "corpo inválido";
  if (typeof scenario.vehicleId !== "string" || scenario.vehicleId === "") return "vehicleId obrigatório";
  if (!fleet.some((vehicle) => vehicle.vehicleId === scenario.vehicleId)) return "vehicleId desconhecido";
  const battery = scenario.batteryPercentage;
  if (battery !== undefined && !(Number.isInteger(battery) && battery >= 0 && battery <= 100)) {
    return "batteryPercentage tem de ser um inteiro entre 0 e 100";
  }
  return null;
}

async function postScenario(request, response, { publisher, fleet }) {
  let scenario;
  try {
    scenario = JSON.parse(await readBody(request));
  } catch {
    return send(response, 400, { status: 400, title: "JSON mal formado" });
  }
  const problem = validate(scenario, fleet);
  if (problem) return send(response, 400, { status: 400, title: problem });

  if (typeof scenario.raw === "string") {
    await publisher.publishRaw(scenario.vehicleId, scenario.raw);
    return send(response, 202, { vehicleId: scenario.vehicleId, raw: scenario.raw });
  }
  const vehicle = fleet.find((candidate) => candidate.vehicleId === scenario.vehicleId);
  const { originAt, batteryPercentage, locked, hubId, shock } = scenario;
  const overrides = { batteryPercentage, locked, hubId, shock };
  const defined = Object.fromEntries(Object.entries(overrides).filter(([, value]) => value !== undefined));
  const reading = { ...toReading({ ...vehicle, ...defined }, () => new Date()), ...(originAt && { originAt }) };
  await publisher.publish(reading);
  return send(response, 202, reading);
}

export function createServer({ brokerUrl, publisher, fleet, isConnected }) {
  return http.createServer((request, response) => {
    if (request.method === "GET" && request.url === "/health") {
      return send(response, 200, {
        service: "marazul-telemetry-simulator",
        status: "UP",
        brokerUrl,
        mqttConnected: isConnected(),
      });
    }
    if (request.method === "POST" && request.url === "/scenarios") {
      return postScenario(request, response, { publisher, fleet }).catch(() =>
        send(response, 502, { status: 502, title: "Falha ao publicar no broker" }),
      );
    }
    return send(response, 404, { status: 404, title: "Not Found" });
  });
}
