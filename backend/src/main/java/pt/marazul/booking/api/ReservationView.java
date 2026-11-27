package pt.marazul.booking.api;

import java.time.Instant;
import java.util.UUID;

public record ReservationView(UUID id, UUID vehicleId, String guestId, Instant startsAt, State state) {
    public enum State { RESERVED, CONVERTED, EXPIRED }
}
