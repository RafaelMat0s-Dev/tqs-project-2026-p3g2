const CHARGE_STEP = 1;
const DRAIN_STEP = 0.5;
const SHOCK_PROBABILITY = 0.001;

export function createFleet({ size, hubs, random }) {
  return Array.from({ length: size }, (_, i) => ({
    vehicleId: `B-${String(i + 1).padStart(2, "0")}`,
    batteryPercentage: 20 + Math.floor(random() * 81),
    locked: true,
    hubId: hubs[(i + 4) % hubs.length],
    shock: false,
  }));
}

export function stepVehicle(vehicle, random) {
  const parked = vehicle.hubId !== null && vehicle.locked;
  const delta = parked ? CHARGE_STEP : -DRAIN_STEP;
  return {
    ...vehicle,
    batteryPercentage: Math.min(100, Math.max(0, vehicle.batteryPercentage + delta)),
    shock: random() < SHOCK_PROBABILITY,
  };
}
