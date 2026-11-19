package pt.marazul.domain;

import java.util.UUID;

public record Vehicle(UUID id, String hubId, String type, int batteryPercentage, Status status) {
    public enum Status { AVAILABLE, RESERVED, IN_USE, MAINTENANCE, RETIRED }
}
