package pt.marazul.booking.domain;

import java.util.UUID;

public record Vehicle(UUID id, String hubId, String type, int batteryPercentage, Status status) {
    public enum Status { AVAILABLE, RESERVED, IN_USE, MAINTENANCE, DISABLED, RETIRED }

    public Vehicle withStatus(Status newStatus) {
        return new Vehicle(id, hubId, type, batteryPercentage, newStatus);
    }
}
