package pt.marazul.fleet.application;

import java.util.UUID;

public class VehicleNotFound extends RuntimeException {
    public VehicleNotFound(UUID id) {
        super("Veículo não encontrado: " + id);
    }
}
