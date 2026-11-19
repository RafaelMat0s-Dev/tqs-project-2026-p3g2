package pt.marazul.domain;

public class ReservationRuleViolation extends RuntimeException {
    private final String rule;

    public ReservationRuleViolation(String rule, String message) {
        super(message);
        this.rule = rule;
    }

    public String rule() {
        return rule;
    }
}
