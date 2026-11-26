export function toReading(vehicle, clock) {
  return {
    vehicleId: vehicle.vehicleId,
    originAt: clock().toISOString(),
    batteryPercentage: Math.round(vehicle.batteryPercentage),
    locked: vehicle.locked,
    hubId: vehicle.hubId,
    shock: vehicle.shock,
  };
}
