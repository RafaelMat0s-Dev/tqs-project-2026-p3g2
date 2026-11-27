package pt.marazul.identity.domain;

import java.time.Duration;
import java.time.Instant;

public record GuestAccessWindow(Instant checkIn, Instant checkOut) {
    private static final Duration GRACE = Duration.ofHours(2);

    public Instant closesAt() {
        return checkOut.plus(GRACE);
    }

    public boolean allowsLoginAt(Instant at) {
        return !at.isBefore(checkIn) && !at.isAfter(closesAt());
    }

    public Instant sessionExpiry(Instant loginAt, Duration maxSession) {
        Instant byDuration = loginAt.plus(maxSession);
        return byDuration.isBefore(closesAt()) ? byDuration : closesAt();
    }

    public IdentityRuleViolation closed() {
        return new IdentityRuleViolation("BR13",
                "O código não é válido neste momento: vale do check-in até 2 horas depois do check-out.");
    }
}
