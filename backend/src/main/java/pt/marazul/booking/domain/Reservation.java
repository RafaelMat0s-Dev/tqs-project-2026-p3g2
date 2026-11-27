package pt.marazul.booking.domain;

import java.time.Instant;
import java.util.UUID;

public record Reservation(UUID id, UUID vehicleId, String guestId, Instant startsAt, Instant endsAt, Status status) {
    public enum Status { RESERVED, CONVERTED, EXPIRED }

    public Reservation(UUID id, UUID vehicleId, String guestId, Instant startsAt, Instant endsAt) {
        this(id, vehicleId, guestId, startsAt, endsAt, Status.RESERVED);
    }

    public Reservation withStatus(Status newStatus) {
        return new Reservation(id, vehicleId, guestId, startsAt, endsAt, newStatus);
    }
}
