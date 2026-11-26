package pt.marazul.identity.application;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginThrottle {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofSeconds(60);
    private static final Duration MIN_RETRY = Duration.ofSeconds(1);

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String origin) {
        Instant now = clock.instant();
        Instant[] oldest = new Instant[1];
        failures.computeIfPresent(origin, (key, recent) -> {
            prune(recent, now);
            if (recent.size() >= MAX_FAILURES) {
                oldest[0] = recent.peekFirst();
            }
            return recent.isEmpty() ? null : recent;
        });
        if (oldest[0] != null) {
            Duration wait = Duration.between(now, oldest[0].plus(WINDOW));
            throw new TooManyLoginAttempts(wait.compareTo(MIN_RETRY) < 0 ? MIN_RETRY : wait);
        }
    }

    int trackedOrigins() {
        return failures.size();
    }

    public void recordFailure(String origin) {
        Instant now = clock.instant();
        failures.compute(origin, (key, recent) -> {
            Deque<Instant> window = recent == null ? new ArrayDeque<>() : recent;
            prune(window, now);
            window.addLast(now);
            return window;
        });
    }

    private static void prune(Deque<Instant> recent, Instant now) {
        while (!recent.isEmpty() && !recent.peekFirst().plus(WINDOW).isAfter(now)) {
            recent.removeFirst();
        }
    }
}
