package pt.marazul.trip.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationPort {
    Optional<ReservationSnapshot> find(UUID reservationId);
    List<ReservationSnapshot> findReservedStartedBefore(Instant instant);
    void markUnlocked(UUID reservationId);
    void expire(UUID reservationId);
}
