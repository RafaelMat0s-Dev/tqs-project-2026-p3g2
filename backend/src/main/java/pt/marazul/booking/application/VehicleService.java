package pt.marazul.booking.application;

import org.springframework.stereotype.Service;
import pt.marazul.booking.domain.FleetRuleViolation;
import pt.marazul.booking.domain.Vehicle;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {
    private static final String FLEET_STATE_RULE = "BR-E4-01";
    private final VehicleRepository vehicles;

    public VehicleService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public Vehicle register(String hubId, String type, int batteryPercentage) {
        if (batteryPercentage < 0 || batteryPercentage > 100) {
            throw new FleetRuleViolation(FLEET_STATE_RULE, "A bateria deve estar entre 0 e 100%.");
        }
        Vehicle vehicle = new Vehicle(UUID.randomUUID(), hubId, type, batteryPercentage, Vehicle.Status.AVAILABLE);
        vehicles.save(vehicle);
        return vehicle;
    }

    public Vehicle changeStatus(UUID id, Vehicle.Status target) {
        Vehicle current = vehicles.findById(id)
                .orElseThrow(() -> new FleetRuleViolation(FLEET_STATE_RULE, "Veículo inexistente."));
        if (current.status() == Vehicle.Status.RETIRED && target != Vehicle.Status.RETIRED) {
            throw new FleetRuleViolation(FLEET_STATE_RULE,
                    "Um veículo abatido não pode voltar a um estado operacional.");
        }
        Vehicle updated = new Vehicle(current.id(), current.hubId(), current.type(), current.batteryPercentage(), target);
        vehicles.save(updated);
        return updated;
    }

    public List<Vehicle> activeVehicles() {
        return vehicles.findAll().stream().filter(vehicle -> vehicle.status() != Vehicle.Status.DISABLED
                && vehicle.status() != Vehicle.Status.RETIRED).toList();
    }
}
