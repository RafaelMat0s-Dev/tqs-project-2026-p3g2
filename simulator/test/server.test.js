import assert from "node:assert/strict";
import { after, before, beforeEach, describe, it } from "node:test";
import { createServer } from "../src/server.js";

describe("simulator server", () => {
  let server;
  let baseUrl;
  let readings;
  let raws;
  let connected;
  const fleet = [
    { vehicleId: "B-17", batteryPercentage: 80, locked: true, hubId: "hub-sao-bras", shock: false },
  ];
  const publisher = {
    publish: async (reading) => readings.push(reading),
    publishRaw: async (vehicleId, text) => raws.push({ vehicleId, text }),
  };

  before(async () => {
    connected = true;
    server = createServer({
      brokerUrl: "mqtt://broker:1883",
      publisher,
      fleet,
      isConnected: () => connected,
    });
    await new Promise((resolve) => server.listen(0, resolve));
    baseUrl = `http://127.0.0.1:${server.address().port}`;
  });

  beforeEach(() => {
    readings = [];
    raws = [];
    connected = true;
  });

  after(() => server.close());

  const post = (body) =>
    fetch(`${baseUrl}/scenarios`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: typeof body === "string" ? body : JSON.stringify(body),
    });

  it("reports UP, the configured broker and the MQTT connection", async () => {
    const response = await fetch(`${baseUrl}/health`);

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      service: "marazul-telemetry-simulator",
      status: "UP",
      brokerUrl: "mqtt://broker:1883",
      mqttConnected: true,
    });
  });

  it("reports mqttConnected false when the broker is down", async () => {
    connected = false;
    const body = await (await fetch(`${baseUrl}/health`)).json();

    assert.equal(body.mqttConnected, false);
    assert.equal(body.status, "UP");
  });

  it("answers 404 for unknown paths", async () => {
    const response = await fetch(`${baseUrl}/nope`);

    assert.equal(response.status, 404);
  });

  it("publishes B-17 at 47 percent completed with the bike state", async () => {
    const response = await post({ vehicleId: "B-17", batteryPercentage: 47 });
    const body = await response.json();

    assert.equal(response.status, 202);
    assert.equal(readings.length, 1);
    assert.deepEqual(body, readings[0]);
    assert.equal(body.vehicleId, "B-17");
    assert.equal(body.batteryPercentage, 47);
    assert.equal(body.locked, true);
    assert.equal(body.hubId, "hub-sao-bras");
    assert.equal(body.shock, false);
    assert.ok(body.originAt);
  });

  it("passes an explicit originAt through unchanged", async () => {
    const response = await post({ vehicleId: "B-17", originAt: "2026-11-27T14:31:05Z" });

    assert.equal(response.status, 202);
    assert.equal(readings[0].originAt, "2026-11-27T14:31:05Z");
  });

  it("overrides locked, hubId and shock when given", async () => {
    await post({ vehicleId: "B-17", locked: false, hubId: null, shock: true });

    assert.equal(readings[0].locked, false);
    assert.equal(readings[0].hubId, null);
    assert.equal(readings[0].shock, true);
  });

  it("publishes a raw invalid JSON text without changes", async () => {
    const response = await post({ vehicleId: "B-17", raw: "{isto não é json" });

    assert.equal(response.status, 202);
    assert.deepEqual(raws, [{ vehicleId: "B-17", text: "{isto não é json" }]);
    assert.equal(readings.length, 0);
  });

  for (const battery of [101, -1]) {
    it(`rejects battery ${battery} with 400`, async () => {
      const response = await post({ vehicleId: "B-17", batteryPercentage: battery });

      assert.equal(response.status, 400);
      assert.equal(readings.length, 0);
    });
  }

  it("rejects malformed JSON with 400", async () => {
    assert.equal((await post("{nope")).status, 400);
  });

  it("rejects an empty vehicleId with 400", async () => {
    assert.equal((await post({ vehicleId: "" })).status, 400);
  });

  it("rejects an unknown vehicleId with 400", async () => {
    const response = await post({ vehicleId: "B-99" });

    assert.equal(response.status, 400);
    assert.equal(readings.length, 0);
  });
});
