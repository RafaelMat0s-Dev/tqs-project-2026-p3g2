function positiveNumber(env, name, fallback) {
  const raw = env[name];
  if (raw === undefined || raw === "") return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} tem de ser um número positivo (recebido "${raw}")`);
  }
  return value;
}

export function readConfig(env) {
  return {
    mqttUrl: env.MQTT_URL || "mqtt://localhost:1883",
    publishRate: positiveNumber(env, "PUBLISH_RATE", 10),
    fleetSize: positiveNumber(env, "FLEET_SIZE", 60),
    port: Number(env.PORT || 8090),
  };
}
