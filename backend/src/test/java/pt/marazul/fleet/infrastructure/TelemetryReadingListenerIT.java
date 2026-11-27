package pt.marazul.fleet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.application.FleetService;
import pt.marazul.support.TestcontainersConfiguration;
import pt.marazul.telemetry.api.TelemetryReadingReceived;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TelemetryReadingListenerIT {
    private static final Instant T = Instant.parse("2026-11-20T14:31:07Z");

    @Autowired ApplicationEventPublisher publisher;
    @Autowired FleetService service;
    @Autowired FleetApi fleetApi;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanTable() {
        jdbc.update("delete from fleet_vehicle");
        service.register("B-17", "BICYCLE", "sao-bras");
    }

    private static TelemetryReadingReceived reading(String code, Instant at, int pct) {
        return new TelemetryReadingReceived(code, at, pct, true, "sao-bras", false);
    }

    @Test
    void readingUpdatesBatteryAndLastReadingAt() {
        publisher.publishEvent(reading("B-17", T, 47));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(fleetApi.findByCode("B-17")).hasValueSatisfying(v -> {
                    assertThat(v.batteryPercentage()).isEqualTo(47);
                    assertThat(v.lastReadingAt()).isEqualTo(T);
                }));
    }

    @Test
    void olderReadingDoesNotChangeBattery() {
        publisher.publishEvent(reading("B-17", T, 47));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(fleetApi.findByCode("B-17").orElseThrow().batteryPercentage()).isEqualTo(47));

        publisher.publishEvent(reading("B-17", T.minusSeconds(5), 30));

        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(fleetApi.findByCode("B-17")).hasValueSatisfying(v -> {
                    assertThat(v.batteryPercentage()).isEqualTo(47);
                    assertThat(v.lastReadingAt()).isEqualTo(T);
                }));
    }

    @Test
    void unknownVehicleCodeIsIgnoredWithoutCreatingVehicle() {
        assertThatCode(() -> publisher.publishEvent(reading("B-99", T, 47))).doesNotThrowAnyException();

        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(fleetApi.findByCode("B-99")).isEqualTo(Optional.empty()));
        assertThat(fleetApi.list(Optional.empty(), Optional.empty())).hasSize(1);
    }
}
