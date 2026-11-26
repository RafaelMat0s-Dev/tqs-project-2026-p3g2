import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, it, mock } from "node:test";
import { createLoop } from "../src/loop.js";

const clock = () => new Date("2026-11-27T14:31:05Z");
const calm = () => 0.5;

function vehicles(n) {
  return Array.from({ length: n }, (_, i) => ({
    vehicleId: `B-${String(i + 1).padStart(2, "0")}`,
    batteryPercentage: 50,
    locked: true,
    hubId: "hub-sao-bras",
    shock: false,
  }));
}

describe("loop", () => {
  let published;
  let publisher;

  beforeEach(() => {
    mock.timers.enable({ apis: ["setInterval", "setTimeout"] });
    published = [];
    publisher = { publish: async (reading) => published.push(reading), publishRaw: async () => {} };
  });

  afterEach(() => mock.timers.reset());

  it("publishes 10 readings in one second at 10 per second", () => {
    const loop = createLoop({ fleet: vehicles(60), publisher, rate: 10, random: calm, clock });
    loop.start();
    mock.timers.tick(1000);
    loop.stop();

    assert.equal(published.length, 10);
  });

  it("publishes one bike every 100 ms, in rotation", () => {
    const loop = createLoop({ fleet: vehicles(3), publisher, rate: 10, random: calm, clock });
    loop.start();
    mock.timers.tick(99);
    assert.equal(published.length, 0);
    mock.timers.tick(1);
    assert.deepEqual(published.map((r) => r.vehicleId), ["B-01"]);
    mock.timers.tick(300);
    loop.stop();

    assert.deepEqual(published.map((r) => r.vehicleId), ["B-01", "B-02", "B-03", "B-01"]);
  });

  it("stamps each reading with the injected clock", () => {
    const loop = createLoop({ fleet: vehicles(2), publisher, rate: 10, random: calm, clock });
    loop.start();
    mock.timers.tick(100);
    loop.stop();

    assert.equal(published[0].originAt, "2026-11-27T14:31:05.000Z");
  });

  it("stops publishing after stop()", () => {
    const loop = createLoop({ fleet: vehicles(3), publisher, rate: 10, random: calm, clock });
    loop.start();
    mock.timers.tick(200);
    loop.stop();
    mock.timers.tick(1000);

    assert.equal(published.length, 2);
  });
});
