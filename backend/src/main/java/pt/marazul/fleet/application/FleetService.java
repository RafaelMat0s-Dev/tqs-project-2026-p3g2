package pt.marazul.fleet.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;
import pt.marazul.shared.domain.BusinessRuleConflict;

@Service
public class FleetService {
    private final VehicleRepository vehicles;

    public FleetService(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public Vehicle register(String code, String type, String baseHubCode) {
        if (vehicles.findByCode(code).isPresent()) {
            throw new BusinessRuleConflict("VEHICLE_CODE", "Já existe um veículo com o código " + code + ".");
        }
        return vehicles.save(Vehicle.register(UUID.randomUUID(), code, type, baseHubCode));
    }

    public Vehicle activate(UUID id) {
        return moveTo(id, VehicleStatus.AVAILABLE);
    }

    public Vehicle deactivate(UUID id) {
        return moveTo(id, VehicleStatus.DISABLED);
    }

    public Vehicle retire(UUID id) {
        return moveTo(id, VehicleStatus.RETIRED);
    }

    public Vehicle markReserved(UUID id) {
        return moveTo(id, VehicleStatus.RESERVED);
    }

    public Vehicle release(UUID id) {
        return moveTo(id, VehicleStatus.AVAILABLE);
    }

    public Vehicle startTrip(UUID id) {
        return moveTo(id, VehicleStatus.IN_USE);
    }

    public Vehicle endTrip(UUID id) {
        return moveTo(id, VehicleStatus.AVAILABLE);
    }

    public Vehicle sendToMaintenance(UUID id) {
        return moveTo(id, VehicleStatus.MAINTENANCE);
    }

    public Vehicle completeMaintenance(UUID id) {
        return moveTo(id, VehicleStatus.AVAILABLE);
    }

    public Optional<Vehicle> findById(UUID id) {
        return vehicles.findById(id);
    }

    public Optional<Vehicle> findByCode(String code) {
        return vehicles.findByCode(code);
    }

    public List<Vehicle> list(Optional<VehicleStatus> status, Optional<String> hub) {
        return vehicles.list(status, hub);
    }

    public void applyReading(String code, int percentage, Instant readingAt) {
        vehicles.updateBatteryIfNewer(code, percentage, readingAt);
    }

    private Vehicle moveTo(UUID id, VehicleStatus target) {
        Vehicle vehicle = vehicles.findById(id).orElseThrow(() -> new VehicleNotFound(id));
        vehicle.moveTo(target);
        if (!vehicles.changeStatus(id, vehicle.status(), target)) {
            throw new BusinessRuleConflict("VEHICLE_STATE_CHANGED",
                    "O estado do veículo " + vehicle.code() + " mudou entretanto; repita a operação.");
        }
        return vehicles.findById(id).orElseThrow(() -> new VehicleNotFound(id));
    }
}
