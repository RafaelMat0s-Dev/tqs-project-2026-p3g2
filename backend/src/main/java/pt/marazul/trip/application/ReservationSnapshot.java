package pt.marazul.trip.application;

import java.time.Instant;
import java.util.UUID;

public record ReservationSnapshot(UUID id, UUID vehicleId, String guestId, Instant startsAt, State state) {
    public enum State { RESERVED, CONVERTED, EXPIRED }
}
