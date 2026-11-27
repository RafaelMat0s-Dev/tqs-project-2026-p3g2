import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { toReading } from "../src/reading.js";

const clock = () => new Date("2026-11-27T14:31:05Z");

describe("toReading", () => {
  const vehicle = { vehicleId: "B-17", batteryPercentage: 47, locked: true, hubId: "sao-bras", shock: false };

  it("returns the telemetry format with originAt from the injected clock", () => {
    assert.deepEqual(toReading(vehicle, clock), {
      vehicleId: "B-17",
      originAt: "2026-11-27T14:31:05.000Z",
      batteryPercentage: 47,
      locked: true,
      hubId: "sao-bras",
      shock: false,
    });
  });

  it("keeps a null hubId for a bike on a trip", () => {
    const riding = { ...vehicle, locked: false, hubId: null };
    assert.equal(toReading(riding, clock).hubId, null);
  });

  it("rounds the battery to an integer", () => {
    const reading = toReading({ ...vehicle, batteryPercentage: 46.6 }, clock);
    assert.equal(reading.batteryPercentage, 47);
  });
});
