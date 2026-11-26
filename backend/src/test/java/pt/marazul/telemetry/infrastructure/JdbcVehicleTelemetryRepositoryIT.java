package pt.marazul.telemetry.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.marazul.support.TestcontainersConfiguration;
import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcVehicleTelemetryRepositoryIT {
    private static final Instant T_14_31_05 = Instant.parse("2026-11-27T14:31:05Z");
    private static final Instant T_14_31_07 = Instant.parse("2026-11-27T14:31:07Z");
    private static final Instant T_14_31_09 = Instant.parse("2026-11-27T14:31:09Z");

    @Autowired
    JdbcVehicleTelemetryRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanTable() {
        jdbc.update("delete from telemetry_vehicle_state");
    }

    @Test
    void firstReadingIsSavedAndFound() {
        var reading = reading(T_14_31_07, 47);

        assertThat(repository.saveIfNewer(reading)).isTrue();

        assertThat(repository.findLatest("B-17")).contains(reading);
    }

    @Test
    void unknownVehicleHasNoState() {
        assertThat(repository.findLatest("B-17")).isEmpty();
    }

    @Test
    void olderReadingIsNotSavedAndStateStays() {
        repository.saveIfNewer(reading(T_14_31_07, 47));

        assertThat(repository.saveIfNewer(reading(T_14_31_05, 30))).isFalse();

        assertThat(repository.findLatest("B-17").orElseThrow().batteryPercentage()).isEqualTo(47);
        assertThat(repository.findLatest("B-17").orElseThrow().originAt()).isEqualTo(T_14_31_07);
    }

    @Test
    void readingWithTheSameInstantIsNotSaved() {
        repository.saveIfNewer(reading(T_14_31_07, 47));

        assertThat(repository.saveIfNewer(reading(T_14_31_07, 30))).isFalse();

        assertThat(repository.findLatest("B-17").orElseThrow().batteryPercentage()).isEqualTo(47);
    }

    @Test
    void newerReadingIsSavedAndStateChanges() {
        repository.saveIfNewer(reading(T_14_31_07, 47));

        assertThat(repository.saveIfNewer(reading(T_14_31_09, 45))).isTrue();

        var latest = repository.findLatest("B-17").orElseThrow();
        assertThat(latest.batteryPercentage()).isEqualTo(45);
        assertThat(latest.originAt()).isEqualTo(T_14_31_09);
    }

    @Test
    void hubCanBeNull() {
        var reading = new TelemetryReading("B-17", T_14_31_07, 47, false, null, true);

        repository.saveIfNewer(reading);

        assertThat(repository.findLatest("B-17")).contains(reading);
    }

    @Test
    void databaseRejectsBatteryAbove100() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into telemetry_vehicle_state (vehicle_code, battery_percentage, locked, hub_id, shock,"
                        + " last_reading_at, updated_at) values ('B-17', 101, true, 'sao-bras', false, now(), now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static TelemetryReading reading(Instant originAt, int battery) {
        return new TelemetryReading("B-17", originAt, battery, true, "sao-bras", false);
    }
}
