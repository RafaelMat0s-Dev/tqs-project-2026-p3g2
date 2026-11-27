package pt.marazul.trip.application;

import org.junit.jupiter.api.Test;
import pt.marazul.trip.domain.Trip;
import pt.marazul.trip.domain.TripRuleViolation;
import pt.marazul.trip.infrastructure.InMemoryTripRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnlockServiceTest {
    private static final Instant START = Instant.parse("2026-12-01T15:02:00Z");
    private final FakeReservationPort reservations = new FakeReservationPort();

    private UnlockService serviceAt(String instant) {
        return new UnlockService(reservations, () -> Duration.ofMinutes(15), new InMemoryTripRepository(),
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void unlockingOnTheLastSecondOfTheWindowStartsTheTrip() {
        UUID id = reservations.add("marta", START, ReservationSnapshot.State.RESERVED);

        Trip trip = serviceAt("2026-12-01T15:17:00Z").unlock(id, "marta");

        assertThat(trip.reservationId()).isEqualTo(id);
        assertThat(trip.startedAt()).isEqualTo(Instant.parse("2026-12-01T15:17:00Z"));
        assertThat(reservations.unlocked).containsExactly(id);
    }

    @Test
    void unlockingOneSecondLateExpiresTheReservationWithBr6() {
        UUID id = reservations.add("marta", START, ReservationSnapshot.State.RESERVED);

        assertThatThrownBy(() -> serviceAt("2026-12-01T15:17:01Z").unlock(id, "marta"))
                .isInstanceOfSatisfying(TripRuleViolation.class, e -> assertThat(e.rule()).isEqualTo("BR6"));
        assertThat(reservations.expired).containsExactly(id);
        assertThat(reservations.unlocked).isEmpty();
    }

    @Test
    void anAlreadyExpiredReservationIsRejectedWithBr6() {
        UUID id = reservations.add("marta", START, ReservationSnapshot.State.EXPIRED);

        assertThatThrownBy(() -> serviceAt("2026-12-01T15:30:00Z").unlock(id, "marta"))
                .isInstanceOfSatisfying(TripRuleViolation.class, e -> assertThat(e.rule()).isEqualTo("BR6"));
        assertThat(reservations.expired).isEmpty();
    }

    @Test
    void aGuestCannotUnlockAnotherGuestsReservation() {
        UUID id = reservations.add("pedro", START, ReservationSnapshot.State.RESERVED);

        assertThatThrownBy(() -> serviceAt("2026-12-01T15:05:00Z").unlock(id, "marta"))
                .isInstanceOf(NotTheGuestsReservation.class);
        assertThat(reservations.unlocked).isEmpty();
    }

    @Test
    void anUnknownOrAlreadyUnlockedReservationIsNotFound() {
        UUID converted = reservations.add("marta", START, ReservationSnapshot.State.CONVERTED);
        UnlockService service = serviceAt("2026-12-01T15:05:00Z");

        assertThatThrownBy(() -> service.unlock(UUID.randomUUID(), "marta")).isInstanceOf(ReservationNotFound.class);
        assertThatThrownBy(() -> service.unlock(converted, "marta")).isInstanceOf(ReservationNotFound.class);
    }
}
