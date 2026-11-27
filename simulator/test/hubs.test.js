import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { HUBS } from "../src/hubs.js";

describe("HUBS", () => {
  it("lists the six real hubs in order", () => {
    assert.deepEqual(HUBS, ["hotel-alvina-mar", "praia-da-ribeira", "sao-bras", "hub-4", "hub-5", "hub-6"]);
  });
});
