package pt.marazul.trip.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PickupWindowTest {
    private final PickupWindow window = new PickupWindow(Instant.parse("2026-12-01T15:02:00Z"), Duration.ofMinutes(15));

    @ParameterizedTest(name = "desbloquear às {0} é aceite: {1}")
    @CsvSource({
            "2026-12-01T15:02:00Z, true",
            "2026-12-01T15:16:59Z, true",
            "2026-12-01T15:17:00Z, true",
            "2026-12-01T15:17:01Z, false"
    })
    void unlockIsAllowedUntilTheEndOfTheWindowInclusive(Instant instant, boolean allowed) {
        assertThat(window.allowsUnlockAt(instant)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({"15, 2026-12-01T15:17:00Z", "10, 2026-12-01T15:12:00Z"})
    void theWindowLengthComesFromTheContract(long minutes, Instant closesAt) {
        assertThat(new PickupWindow(Instant.parse("2026-12-01T15:02:00Z"), Duration.ofMinutes(minutes)).closesAt()).isEqualTo(closesAt);
    }

    @Test
    void aClosedWindowIsABr6Violation() {
        assertThat(window.closed().rule()).isEqualTo("BR6");
    }
}
