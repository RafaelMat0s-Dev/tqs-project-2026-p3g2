package pt.marazul.fleet.domain;

import java.util.Set;

public enum VehicleStatus {
    DISABLED("Inativa"),
    AVAILABLE("Disponível"),
    RESERVED("Reservada"),
    IN_USE("Em uso"),
    MAINTENANCE("Manutenção"),
    RETIRED("Abatida");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canMoveTo(VehicleStatus target) {
        return switch (this) {
            case DISABLED -> Set.of(AVAILABLE, RETIRED, MAINTENANCE).contains(target);
            case AVAILABLE -> Set.of(DISABLED, RESERVED, MAINTENANCE).contains(target);
            case RESERVED -> Set.of(AVAILABLE, IN_USE, MAINTENANCE).contains(target);
            case IN_USE -> Set.of(AVAILABLE, MAINTENANCE).contains(target);
            case MAINTENANCE -> Set.of(AVAILABLE, RETIRED).contains(target);
            case RETIRED -> false;
        };
    }
}
