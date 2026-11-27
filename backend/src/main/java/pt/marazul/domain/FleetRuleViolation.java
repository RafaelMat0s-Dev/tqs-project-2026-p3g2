package pt.marazul.domain;

public class FleetRuleViolation extends RuntimeException {
    private final String rule;

    public FleetRuleViolation(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    public String rule() {
        return rule;
    }
}
