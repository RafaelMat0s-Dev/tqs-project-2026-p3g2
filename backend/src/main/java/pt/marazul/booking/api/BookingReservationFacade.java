package pt.marazul.booking.api;

import org.springframework.stereotype.Component;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BookingReservationFacade implements ReservationFacade {
    private final ReservationService service;

    public BookingReservationFacade(ReservationService service) {
        this.service = service;
    }

    @Override
    public Optional<ReservationView> find(UUID reservationId) {
        return service.find(reservationId).map(BookingReservationFacade::view);
    }

    @Override
    public List<ReservationView> findReservedStartedBefore(Instant instant) {
        return service.findReservedStartedBefore(instant).stream().map(BookingReservationFacade::view).toList();
    }

    @Override
    public void markUnlocked(UUID reservationId) {
        service.markUnlocked(reservationId);
    }

    @Override
    public void expire(UUID reservationId) {
        service.expire(reservationId);
    }

    private static ReservationView view(Reservation reservation) {
        return new ReservationView(reservation.id(), reservation.vehicleId(), reservation.guestId(),
                reservation.startsAt(), ReservationView.State.valueOf(reservation.status().name()));
    }
}
