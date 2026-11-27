package pt.marazul.booking.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.booking.application.ReservationRepository;
import pt.marazul.booking.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {
    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public List<Reservation> findOverlapping(UUID vehicleId, Instant startsAt, Instant endsAt) {
        return notExpired()
                .filter(r -> r.vehicleId().equals(vehicleId))
                .filter(r -> r.startsAt().isBefore(endsAt) && startsAt.isBefore(r.endsAt()))
                .toList();
    }

    @Override
    public List<Reservation> findActiveForGuest(String guestId, Instant startsAt, Instant endsAt) {
        return notExpired()
                .filter(r -> r.guestId().equals(guestId))
                .filter(r -> r.startsAt().isBefore(endsAt) && startsAt.isBefore(r.endsAt()))
                .toList();
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public List<Reservation> findReservedStartedBefore(Instant instant) {
        return reservations.values().stream()
                .filter(r -> r.status() == Reservation.Status.RESERVED)
                .filter(r -> r.startsAt().isBefore(instant))
                .toList();
    }

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    private Stream<Reservation> notExpired() {
        return reservations.values().stream().filter(r -> r.status() != Reservation.Status.EXPIRED);
    }
}
