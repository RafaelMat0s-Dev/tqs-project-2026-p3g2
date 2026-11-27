package pt.marazul.trip.application;

import java.util.UUID;

public class ReservationNotFound extends RuntimeException {
    public ReservationNotFound(UUID reservationId) {
        super("Não existe uma reserva por desbloquear com o identificador " + reservationId + ".");
    }
}
