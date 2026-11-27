package pt.marazul.trip.application;

public class NotTheGuestsReservation extends RuntimeException {
    public NotTheGuestsReservation() {
        super("Esta reserva pertence a outro hóspede.");
    }
}
