package pt.marazul.telemetry.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryReadingTest {
    private static final Instant ORIGIN = Instant.parse("2026-11-27T14:31:07Z");

    private static TelemetryReading reading(String code, Instant at, int battery) {
        return new TelemetryReading(code, at, battery, true, "sao-bras", false);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 47, 100})
    void acceptsBatteryWithinZeroAndOneHundred(int battery) {
        assertThat(reading("B-17", ORIGIN, battery).batteryPercentage()).isEqualTo(battery);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void rejectsBatteryOutsideZeroAndOneHundred(int battery) {
        assertThatThrownBy(() -> reading("B-17", ORIGIN, battery))
                .isInstanceOf(InvalidTelemetryReading.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingOrBlankVehicleCode(String code) {
        assertThatThrownBy(() -> reading(code, ORIGIN, 47))
                .isInstanceOf(InvalidTelemetryReading.class);
    }

    @Test
    void rejectsMissingOriginInstant() {
        assertThatThrownBy(() -> reading("B-17", null, 47))
                .isInstanceOf(InvalidTelemetryReading.class);
    }

    @Test
    void acceptsMissingHubId() {
        var r = new TelemetryReading("B-17", ORIGIN, 47, false, null, false);
        assertThat(r.hubId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-27T14:31:05Z", "2026-11-27T14:31:07Z"})
    void olderOrEqualReadingIsNotNewer(String other) {
        assertThat(reading("B-17", Instant.parse(other), 30).isNewerThan(reading("B-17", ORIGIN, 47))).isFalse();
    }

    @Test
    void laterReadingIsNewer() {
        var later = reading("B-17", Instant.parse("2026-11-27T14:31:09Z"), 30);
        assertThat(later.isNewerThan(reading("B-17", ORIGIN, 47))).isTrue();
    }
}
