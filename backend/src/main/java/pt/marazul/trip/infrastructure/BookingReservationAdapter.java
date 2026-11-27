package pt.marazul.trip.infrastructure;

import org.springframework.stereotype.Component;
import pt.marazul.booking.api.ReservationFacade;
import pt.marazul.booking.api.ReservationView;
import pt.marazul.trip.application.ReservationPort;
import pt.marazul.trip.application.ReservationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BookingReservationAdapter implements ReservationPort {
    private final ReservationFacade booking;

    public BookingReservationAdapter(ReservationFacade booking) {
        this.booking = booking;
    }

    @Override
    public Optional<ReservationSnapshot> find(UUID reservationId) {
        return booking.find(reservationId).map(BookingReservationAdapter::snapshot);
    }

    @Override
    public List<ReservationSnapshot> findReservedStartedBefore(Instant instant) {
        return booking.findReservedStartedBefore(instant).stream().map(BookingReservationAdapter::snapshot).toList();
    }

    @Override
    public void markUnlocked(UUID reservationId) {
        booking.markUnlocked(reservationId);
    }

    @Override
    public void expire(UUID reservationId) {
        booking.expire(reservationId);
    }

    private static ReservationSnapshot snapshot(ReservationView view) {
        return new ReservationSnapshot(view.id(), view.vehicleId(), view.guestId(), view.startsAt(),
                ReservationSnapshot.State.valueOf(view.state().name()));
    }
}
