package pt.marazul.identity.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

public class IdentityRuleViolation extends BusinessRuleViolation {
    public IdentityRuleViolation(String rule, String message) {
        super(rule, message);
    }
}
