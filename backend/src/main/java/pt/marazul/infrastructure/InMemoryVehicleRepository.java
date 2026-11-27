package pt.marazul.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.application.VehicleRepository;
import pt.marazul.domain.Vehicle;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryVehicleRepository implements VehicleRepository {
    private final Map<UUID, Vehicle> vehicles = new ConcurrentHashMap<>();

    @Override
    public Optional<Vehicle> findById(UUID id) {
        return Optional.ofNullable(vehicles.get(id));
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicles.values().stream().toList();
    }

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.id(), vehicle);
    }
}
