import { createServer } from "./server.js";

const port = Number(process.env.PORT ?? 8090);
const brokerUrl = process.env.MQTT_URL ?? "mqtt://localhost:1883";

createServer({ brokerUrl }).listen(port, () => {
  console.log(`Telemetry simulator listening on ${port} (broker ${brokerUrl})`);
});
