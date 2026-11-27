package pt.marazul.shared.domain;

public class BusinessRuleConflict extends BusinessRuleViolation {
    public BusinessRuleConflict(String rule, String message) {
        super(rule, message);
    }
}
