package pt.marazul.trip.application;

import org.springframework.stereotype.Service;
import pt.marazul.trip.domain.PickupWindow;
import pt.marazul.trip.domain.Trip;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class UnlockService {
    private final ReservationPort reservations;
    private final PickupWindowPort contract;
    private final TripRepository trips;
    private final Clock clock;

    public UnlockService(ReservationPort reservations, PickupWindowPort contract, TripRepository trips, Clock clock) {
        this.reservations = reservations;
        this.contract = contract;
        this.trips = trips;
        this.clock = clock;
    }

    public Trip unlock(UUID reservationId, String guestId) {
        ReservationSnapshot reservation = reservations.find(reservationId)
                .filter(r -> r.state() != ReservationSnapshot.State.CONVERTED)
                .orElseThrow(() -> new ReservationNotFound(reservationId));
        if (!reservation.guestId().equals(guestId)) {
            throw new NotTheGuestsReservation();
        }
        PickupWindow window = new PickupWindow(reservation.startsAt(), contract.pickupWindow());
        Instant now = clock.instant();
        if (reservation.state() == ReservationSnapshot.State.EXPIRED) {
            throw window.closed();
        }
        if (!window.allowsUnlockAt(now)) {
            reservations.expire(reservationId);
            throw window.closed();
        }
        reservations.markUnlocked(reservationId);
        return trips.save(new Trip(UUID.randomUUID(), reservationId, reservation.vehicleId(), guestId, now));
    }
}
