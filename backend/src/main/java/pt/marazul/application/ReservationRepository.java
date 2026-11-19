package pt.marazul.application;

import pt.marazul.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository {
    List<Reservation> findOverlapping(UUID vehicleId, Instant startsAt, Instant endsAt);
    List<Reservation> findActiveForGuest(String guestId, Instant startsAt, Instant endsAt);
    Reservation save(Reservation reservation);
}
