package pt.marazul.application;

import pt.marazul.domain.Vehicle;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface VehicleRepository {
    Optional<Vehicle> findById(UUID id);
    List<Vehicle> findAll();
    void save(Vehicle vehicle);
}
