package pt.marazul.telemetry.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pt.marazul.telemetry.application.VehicleTelemetryRepository;
import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
class JdbcVehicleTelemetryRepository implements VehicleTelemetryRepository {
    private static final String UPSERT = """
            INSERT INTO telemetry_vehicle_state
                (vehicle_code, battery_percentage, locked, hub_id, shock, last_reading_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (vehicle_code) DO UPDATE SET
                battery_percentage = EXCLUDED.battery_percentage,
                locked = EXCLUDED.locked,
                hub_id = EXCLUDED.hub_id,
                shock = EXCLUDED.shock,
                last_reading_at = EXCLUDED.last_reading_at,
                updated_at = EXCLUDED.updated_at
            WHERE telemetry_vehicle_state.last_reading_at < EXCLUDED.last_reading_at
            """;
    private static final String FIND = """
            SELECT vehicle_code, battery_percentage, locked, hub_id, shock, last_reading_at
            FROM telemetry_vehicle_state WHERE vehicle_code = ?
            """;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    JdbcVehicleTelemetryRepository(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public boolean saveIfNewer(TelemetryReading reading) {
        return jdbc.update(UPSERT, reading.vehicleCode(), reading.batteryPercentage(), reading.locked(),
                reading.hubId(), reading.shock(), utc(reading.originAt()), utc(clock.instant())) > 0;
    }

    @Override
    public Optional<TelemetryReading> findLatest(String vehicleCode) {
        return jdbc.query(FIND, (rs, row) -> new TelemetryReading(
                rs.getString("vehicle_code"),
                rs.getObject("last_reading_at", OffsetDateTime.class).toInstant(),
                rs.getInt("battery_percentage"),
                rs.getBoolean("locked"),
                rs.getString("hub_id"),
                rs.getBoolean("shock")), vehicleCode).stream().findFirst();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
