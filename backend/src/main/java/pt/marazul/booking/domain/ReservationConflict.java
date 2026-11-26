package pt.marazul.booking.domain;

import pt.marazul.shared.domain.BusinessRuleConflict;

public class ReservationConflict extends BusinessRuleConflict {
    public ReservationConflict(String message) {
        super("BR7", message);
    }
}
