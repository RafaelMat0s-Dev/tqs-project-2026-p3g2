package pt.marazul.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.application.ReservationRepository;
import pt.marazul.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {
    private final List<Reservation> reservations = new CopyOnWriteArrayList<>();

    @Override
    public List<Reservation> findOverlapping(UUID vehicleId, Instant startsAt, Instant endsAt) {
        return reservations.stream()
                .filter(r -> r.vehicleId().equals(vehicleId))
                .filter(r -> r.startsAt().isBefore(endsAt) && startsAt.isBefore(r.endsAt()))
                .toList();
    }

    @Override
    public List<Reservation> findActiveForGuest(String guestId, Instant startsAt, Instant endsAt) {
        return reservations.stream()
                .filter(r -> r.guestId().equals(guestId))
                .filter(r -> r.startsAt().isBefore(endsAt) && startsAt.isBefore(r.endsAt()))
                .toList();
    }

    @Override
    public Reservation save(Reservation reservation) {
        reservations.add(reservation);
        return reservation;
    }
}
