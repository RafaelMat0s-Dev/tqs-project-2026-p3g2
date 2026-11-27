package pt.marazul.booking.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.marazul.booking.application.VehicleRepository;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.api.VehicleView;

@Repository
public class FleetVehicleRepository implements VehicleRepository {
    private final FleetApi fleet;

    public FleetVehicleRepository(FleetApi fleet) {
        this.fleet = fleet;
    }

    @Override
    public Optional<Vehicle> findById(UUID id) {
        return fleet.findById(id).map(FleetVehicleRepository::toVehicle);
    }

    @Override
    public List<Vehicle> findAll() {
        return fleet.list(Optional.empty(), Optional.empty()).stream()
                .map(FleetVehicleRepository::toVehicle).toList();
    }

    @Override
    public void save(Vehicle vehicle) {
        UUID id = vehicle.id();
        String from = fleet.findById(id)
                .orElseThrow(() -> new IllegalStateException("Veículo desconhecido na frota: " + id))
                .status().name();
        String to = vehicle.status().name();
        if (from.equals(to)) {
            return;
        }
        switch (from + ">" + to) {
            case "AVAILABLE>RESERVED" -> fleet.markReserved(id);
            case "RESERVED>AVAILABLE" -> fleet.release(id);
            case "RESERVED>IN_USE" -> fleet.startTrip(id);
            case "IN_USE>AVAILABLE" -> fleet.endTrip(id);
            default -> {
                if (!"MAINTENANCE".equals(to)) {
                    throw new IllegalStateException("Transição não suportada: " + from + " -> " + to);
                }
                fleet.sendToMaintenance(id);
            }
        }
    }

    private static Vehicle toVehicle(VehicleView v) {
        return new Vehicle(v.id(), v.baseHubCode(), v.type(),
                v.batteryPercentage() == null ? 0 : v.batteryPercentage(),
                Vehicle.Status.valueOf(v.status().name()));
    }
}
