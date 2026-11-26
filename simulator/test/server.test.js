import assert from "node:assert/strict";
import { after, before, describe, it } from "node:test";
import { createServer } from "../src/server.js";

describe("simulator health server", () => {
  let server;
  let baseUrl;

  before(async () => {
    server = createServer({ brokerUrl: "mqtt://broker:1883" });
    await new Promise((resolve) => server.listen(0, resolve));
    baseUrl = `http://127.0.0.1:${server.address().port}`;
  });

  after(() => server.close());

  it("reports UP and the configured broker", async () => {
    const response = await fetch(`${baseUrl}/health`);

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      service: "marazul-telemetry-simulator",
      status: "UP",
      brokerUrl: "mqtt://broker:1883",
    });
  });

  it("answers 404 for unknown paths", async () => {
    const response = await fetch(`${baseUrl}/nope`);

    assert.equal(response.status, 404);
  });
});
