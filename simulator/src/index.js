import mqtt from "mqtt";
import { readConfig } from "./config.js";
import { createFleet } from "./fleet.js";
import { HUBS } from "./hubs.js";
import { createLoop } from "./loop.js";
import { createPublisher } from "./publisher.js";
import { createServer } from "./server.js";

let config;
try {
  config = readConfig(process.env);
} catch (error) {
  console.error(error.message);
  process.exit(1);
}

const client = mqtt.connect(config.mqttUrl);
client.on("error", (error) => console.error(`MQTT: ${error.message}`));

const random = Math.random;
const clock = () => new Date();
const fleet = createFleet({ size: config.fleetSize, hubs: HUBS, random });
const publisher = createPublisher(client);

createLoop({ fleet, publisher, rate: config.publishRate, random, clock }).start();
createServer({
  brokerUrl: config.mqttUrl,
  publisher,
  fleet,
  isConnected: () => client.connected,
}).listen(config.port, () => {
  console.log(`Telemetry simulator listening on ${config.port} (broker ${config.mqttUrl})`);
});
