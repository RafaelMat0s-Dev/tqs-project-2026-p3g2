package pt.marazul.trip.domain;

import java.time.Duration;
import java.time.Instant;

public record PickupWindow(Instant reservationStart, Duration length) {
    public Instant closesAt() {
        return reservationStart.plus(length);
    }

    public boolean allowsUnlockAt(Instant instant) {
        return !instant.isAfter(closesAt());
    }

    public TripRuleViolation closed() {
        return new TripRuleViolation("BR6",
                "A janela de levantamento de " + length.toMinutes() + " minutos terminou; a reserva expirou.");
    }
}
