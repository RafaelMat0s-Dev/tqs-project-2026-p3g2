export function createPublisher(client) {
  const send = (vehicleId, payload) =>
    new Promise((resolve, reject) => {
      client.publish(`marazul/vehicles/${vehicleId}/telemetry`, payload, { qos: 1 }, (error) =>
        error ? reject(error) : resolve(),
      );
    });

  return {
    publish: (reading) => send(reading.vehicleId, JSON.stringify(reading)),
    publishRaw: (vehicleId, text) => send(vehicleId, text),
  };
}
