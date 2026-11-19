package pt.marazul.domain;

public class ReservationConflict extends ReservationRuleViolation {
    public ReservationConflict(String message) {
        super("BR7", message);
    }
}
