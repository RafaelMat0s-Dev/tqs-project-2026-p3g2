import { stepVehicle } from "./fleet.js";
import { toReading } from "./reading.js";

export function createLoop({ fleet, publisher, rate, random, clock }) {
  let timer = null;
  let next = 0;

  const tick = () => {
    fleet[next] = stepVehicle(fleet[next], random);
    const { vehicleId } = fleet[next];
    publisher.publish(toReading(fleet[next], clock)).catch((error) => {
      console.error(`Falha ao publicar ${vehicleId}: ${error.message}`);
    });
    next = (next + 1) % fleet.length;
  };

  return {
    start() {
      timer = setInterval(tick, 1000 / rate);
    },
    stop() {
      clearInterval(timer);
    },
  };
}
