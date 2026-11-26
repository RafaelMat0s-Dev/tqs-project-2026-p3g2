package pt.marazul.trip.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class FakeReservationPort implements ReservationPort {
    final Map<UUID, ReservationSnapshot> reservations = new HashMap<>();
    final List<UUID> unlocked = new ArrayList<>();
    final List<UUID> expired = new ArrayList<>();

    UUID add(String guestId, Instant startsAt, ReservationSnapshot.State state) {
        UUID id = UUID.randomUUID();
        reservations.put(id, new ReservationSnapshot(id, UUID.randomUUID(), guestId, startsAt, state));
        return id;
    }

    @Override
    public Optional<ReservationSnapshot> find(UUID reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<ReservationSnapshot> findReservedStartedBefore(Instant instant) {
        return reservations.values().stream()
                .filter(r -> r.state() == ReservationSnapshot.State.RESERVED && r.startsAt().isBefore(instant))
                .toList();
    }

    @Override
    public void markUnlocked(UUID reservationId) {
        unlocked.add(reservationId);
    }

    @Override
    public void expire(UUID reservationId) {
        expired.add(reservationId);
    }
}
