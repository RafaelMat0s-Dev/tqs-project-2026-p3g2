import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";
import { createPublisher } from "../src/publisher.js";

describe("publisher", () => {
  let calls;
  let publisher;

  beforeEach(() => {
    calls = [];
    const client = {
      publish(topic, payload, opts, cb) {
        calls.push({ topic, payload, opts });
        if (cb) cb(null);
      },
    };
    publisher = createPublisher(client);
  });

  it("publishes a reading on the vehicle topic with qos 1", async () => {
    const reading = {
      vehicleId: "B-17",
      originAt: "2026-11-27T14:31:05Z",
      batteryPercentage: 47,
      locked: true,
      hubId: "hub-sao-bras",
      shock: false,
    };

    await publisher.publish(reading);

    assert.equal(calls.length, 1);
    assert.equal(calls[0].topic, "marazul/vehicles/B-17/telemetry");
    assert.deepEqual(calls[0].opts, { qos: 1 });
    assert.deepEqual(JSON.parse(calls[0].payload), reading);
  });

  it("publishes the raw text exactly as received", async () => {
    await publisher.publishRaw("B-17", "{isto não é json");

    assert.equal(calls[0].topic, "marazul/vehicles/B-17/telemetry");
    assert.deepEqual(calls[0].opts, { qos: 1 });
    assert.equal(String(calls[0].payload), "{isto não é json");
  });
});
