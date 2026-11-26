import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { createFleet, stepVehicle } from "../src/fleet.js";
import { HUBS } from "../src/hubs.js";


function seeded(seed) {
  let state = seed;
  return () => {
    state = (state * 1664525 + 1013904223) % 4294967296;
    return state / 4294967296;
  };
}

describe("createFleet", () => {
  const fleet = createFleet({ size: 60, hubs: HUBS, random: seeded(7) });

  it("creates B-01 to B-60", () => {
    assert.equal(fleet.length, 60);
    assert.equal(fleet[0].vehicleId, "B-01");
    assert.equal(fleet[16].vehicleId, "B-17");
    assert.equal(fleet[59].vehicleId, "B-60");
  });

  it("spreads the bikes over every hub", () => {
    const used = new Set(fleet.map((vehicle) => vehicle.hubId));
    assert.deepEqual([...used].sort(), [...HUBS].sort());
  });

  it("puts B-17 at the sao-bras hub", () => {
    assert.equal(fleet[16].hubId, "sao-bras");
  });

  it("balances the bikes, 10 per hub", () => {
    for (const hub of HUBS) {
      assert.equal(fleet.filter((vehicle) => vehicle.hubId === hub).length, 10);
    }
  });

  it("starts every battery between 20 and 100", () => {
    for (const vehicle of fleet) {
      assert.ok(vehicle.batteryPercentage >= 20 && vehicle.batteryPercentage <= 100);
    }
  });
});

describe("stepVehicle", () => {
  const calm = () => 0.5; // never triggers a rare event

  it("charges a bike parked at a hub", () => {
    const parked = { vehicleId: "B-17", batteryPercentage: 40, locked: true, hubId: "sao-bras", shock: false };
    assert.ok(stepVehicle(parked, calm).batteryPercentage > 40);
  });

  it("drains a bike on a trip", () => {
    const riding = { vehicleId: "B-17", batteryPercentage: 40, locked: false, hubId: null, shock: false };
    assert.ok(stepVehicle(riding, calm).batteryPercentage < 40);
  });

  it("keeps a full parked battery at 100", () => {
    const parked = { vehicleId: "B-17", batteryPercentage: 100, locked: true, hubId: "sao-bras", shock: false };
    assert.equal(stepVehicle(parked, calm).batteryPercentage, 100);
  });

  it("keeps an empty battery on a trip at 0", () => {
    const riding = { vehicleId: "B-17", batteryPercentage: 0, locked: false, hubId: null, shock: false };
    assert.equal(stepVehicle(riding, calm).batteryPercentage, 0);
  });

  it("keeps the battery within 0 and 100 over many random steps", () => {
    const random = seeded(42);
    let vehicle = createFleet({ size: 1, hubs: HUBS, random })[0];
    for (let i = 0; i < 5000; i += 1) {
      vehicle = stepVehicle(vehicle, random);
      assert.ok(vehicle.batteryPercentage >= 0 && vehicle.batteryPercentage <= 100);
    }
  });

  it("does not change the state or raise a shock when nothing rare happens", () => {
    const parked = { vehicleId: "B-17", batteryPercentage: 40, locked: true, hubId: "sao-bras", shock: false };
    const next = stepVehicle(parked, calm);
    assert.equal(next.locked, true);
    assert.equal(next.hubId, "sao-bras");
    assert.equal(next.shock, false);
  });

  it("raises a shock only on a very rare draw", () => {
    const parked = { vehicleId: "B-17", batteryPercentage: 40, locked: true, hubId: "sao-bras", shock: false };
    assert.equal(stepVehicle(parked, () => 0).shock, true);
  });
});
