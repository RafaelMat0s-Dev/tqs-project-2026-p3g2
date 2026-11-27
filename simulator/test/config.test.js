import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { readConfig } from "../src/config.js";

describe("readConfig", () => {
  it("uses the defaults for an empty environment", () => {
    assert.deepEqual(readConfig({}), {
      mqttUrl: "mqtt://localhost:1883",
      publishRate: 10,
      fleetSize: 60,
      port: 8090,
    });
  });

  it("converts valid values to numbers", () => {
    const config = readConfig({
      MQTT_URL: "mqtt://broker:1883",
      PUBLISH_RATE: "25",
      FLEET_SIZE: "12",
      PORT: "9000",
    });

    assert.deepEqual(config, {
      mqttUrl: "mqtt://broker:1883",
      publishRate: 25,
      fleetSize: 12,
      port: 9000,
    });
  });

  it("treats an empty string as not defined", () => {
    const config = readConfig({ PUBLISH_RATE: "", FLEET_SIZE: "" });

    assert.equal(config.publishRate, 10);
    assert.equal(config.fleetSize, 60);
  });

  for (const name of ["PUBLISH_RATE", "FLEET_SIZE"]) {
    for (const value of ["0", "-5", "abc"]) {
      it(`rejects ${name}=${JSON.stringify(value)} naming the variable`, () => {
        assert.throws(() => readConfig({ [name]: value }), (error) => {
          assert.ok(error instanceof Error);
          assert.match(error.message, new RegExp(name));
          return true;
        });
      });
    }
  }
});
