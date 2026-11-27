package pt.marazul.trip.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationExpiryServiceTest {
    @ParameterizedTest(name = "às {0} a reserva das 15h02 expira: {1}")
    @CsvSource({"2026-12-01T15:17:00Z, false", "2026-12-01T15:17:01Z, true"})
    void expiresReservationsOnlyAfterTheWindowCloses(Instant now, boolean expires) {
        FakeReservationPort reservations = new FakeReservationPort();
        UUID id = reservations.add("marta", Instant.parse("2026-12-01T15:02:00Z"), ReservationSnapshot.State.RESERVED);
        reservations.add("pedro", Instant.parse("2026-12-01T15:02:00Z"), ReservationSnapshot.State.CONVERTED);

        int count = new ReservationExpiryService(reservations, () -> Duration.ofMinutes(15), Clock.fixed(now, ZoneOffset.UTC))
                .expireOverdue();

        assertThat(count).isEqualTo(expires ? 1 : 0);
        assertThat(reservations.expired).isEqualTo(expires ? List.of(id) : List.of());
    }
}
