package pt.marazul.booking.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationFacade {
    Optional<ReservationView> find(UUID reservationId);
    List<ReservationView> findReservedStartedBefore(Instant instant);
    void markUnlocked(UUID reservationId);
    void expire(UUID reservationId);
}
