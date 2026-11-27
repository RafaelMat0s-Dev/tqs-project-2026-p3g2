package pt.marazul.fleet.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pt.marazul.fleet.domain.VehicleStatus;

public interface FleetVehicleJpaRepository extends JpaRepository<FleetVehicleEntity, UUID> {
    Optional<FleetVehicleEntity> findByCode(String code);

    @Query("select v from FleetVehicleEntity v where (:status is null or v.status = :status) "
            + "and (:hub is null or v.baseHubCode = :hub) order by v.code")
    List<FleetVehicleEntity> search(@Param("status") VehicleStatus status, @Param("hub") String hub);

    @Transactional
    @Modifying
    @Query("update FleetVehicleEntity v set v.batteryPercentage = :pct, v.lastReadingAt = :at "
            + "where v.code = :code and (v.lastReadingAt is null or v.lastReadingAt < :at)")
    int updateBatteryIfNewer(@Param("code") String code, @Param("pct") int pct, @Param("at") Instant at);

    @Transactional
    @Modifying
    @Query(value = "update fleet_vehicle set status = :to, version = version + 1 where id = :id and status = :from",
            nativeQuery = true)
    int changeStatus(@Param("id") UUID id, @Param("from") String from, @Param("to") String to);
}
