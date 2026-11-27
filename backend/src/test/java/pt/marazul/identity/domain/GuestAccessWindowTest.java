package pt.marazul.identity.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GuestAccessWindowTest {
    private final GuestAccessWindow window = new GuestAccessWindow(
            Instant.parse("2026-11-24T15:00:00Z"), Instant.parse("2026-12-01T11:00:00Z"));

    @ParameterizedTest
    @CsvSource({
            "2026-12-01T13:00:00Z, true",
            "2026-12-01T13:00:01Z, false",
            "2026-12-01T13:01:00Z, false",
            "2026-11-24T14:59:59Z, false",
            "2026-11-24T15:00:00Z, true"
    })
    void loginIsAllowedFromCheckInUntilTwoHoursAfterCheckOut(String at, boolean expected) {
        assertThat(window.allowsLoginAt(Instant.parse(at))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-12-01T12:50:00Z, 2026-12-01T13:00:00Z",
            "2026-11-30T09:00:00Z, 2026-11-30T17:00:00Z"
    })
    void sessionEndsAtTheEarlierOfMaxSessionAndWindowClose(String loginAt, String expected) {
        assertThat(window.sessionExpiry(Instant.parse(loginAt), Duration.ofHours(8))).isEqualTo(Instant.parse(expected));
    }

    @Test
    void closedIsBr13() {
        assertThat(window.closed().rule()).isEqualTo("BR13");
    }

    @Test
    void stayExposesItsWindow() {
        Stay stay = new Stay(java.util.UUID.randomUUID(), "ref", window.checkIn(), window.checkOut());
        assertThat(stay.accessWindow()).isEqualTo(window);
    }
}
