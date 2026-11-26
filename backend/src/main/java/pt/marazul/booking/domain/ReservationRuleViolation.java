package pt.marazul.booking.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

public class ReservationRuleViolation extends BusinessRuleViolation {
    public ReservationRuleViolation(String rule, String message) {
        super(rule, message);
    }
}
