import http from "node:http";

export function createServer({ brokerUrl }) {
  return http.createServer((request, response) => {
    if (request.method === "GET" && request.url === "/health") {
      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify({ service: "marazul-telemetry-simulator", status: "UP", brokerUrl }));
      return;
    }
    response.writeHead(404, { "content-type": "application/json" });
    response.end(JSON.stringify({ status: 404, title: "Not Found" }));
  });
}
