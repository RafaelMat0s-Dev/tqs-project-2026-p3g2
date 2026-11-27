package pt.marazul.booking.application;

import pt.marazul.booking.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    List<Reservation> findOverlapping(UUID vehicleId, Instant startsAt, Instant endsAt);
    List<Reservation> findActiveForGuest(String guestId, Instant startsAt, Instant endsAt);
    Optional<Reservation> findById(UUID id);
    List<Reservation> findReservedStartedBefore(Instant instant);
    Reservation save(Reservation reservation);
}
