package pt.marazul.identity.application;

import java.time.Duration;

public class TooManyLoginAttempts extends RuntimeException {
    private final Duration retryAfter;

    public TooManyLoginAttempts(Duration retryAfter) {
        super("Demasiadas tentativas. Aguarde um minuto e tente novamente.");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
