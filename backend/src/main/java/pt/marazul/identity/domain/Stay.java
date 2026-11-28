package pt.marazul.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record Stay(UUID id, String externalRef, Instant checkIn, Instant checkOut) {
    public GuestAccessWindow accessWindow() {
        return new GuestAccessWindow(checkIn, checkOut);
    }
}
