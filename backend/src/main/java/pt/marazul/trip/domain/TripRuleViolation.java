package pt.marazul.trip.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

public class TripRuleViolation extends BusinessRuleViolation {
    public TripRuleViolation(String rule, String message) {
        super(rule, message);
    }
}
