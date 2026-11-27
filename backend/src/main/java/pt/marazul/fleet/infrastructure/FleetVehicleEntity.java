package pt.marazul.fleet.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;

@Entity
@Table(name = "fleet_vehicle")
public class FleetVehicleEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String type;
    @Column(name = "base_hub_code", nullable = false)
    private String baseHubCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "battery_percentage")
    private Integer batteryPercentage;
    @Column(name = "last_reading_at")
    private Instant lastReadingAt;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FleetVehicleEntity() { }

    FleetVehicleEntity(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    void copyFrom(Vehicle v) {
        code = v.code();
        type = v.type();
        baseHubCode = v.baseHubCode();
        status = v.status();
        batteryPercentage = v.batteryPercentage();
        lastReadingAt = v.lastReadingAt();
    }

    Vehicle toDomain() {
        return new Vehicle(id, code, type, baseHubCode, status, batteryPercentage, lastReadingAt);
    }
}
