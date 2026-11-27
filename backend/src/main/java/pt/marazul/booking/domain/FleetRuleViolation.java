package pt.marazul.booking.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

public class FleetRuleViolation extends BusinessRuleViolation {
    public FleetRuleViolation(String rule, String message) {
        super(rule, message);
    }
}
