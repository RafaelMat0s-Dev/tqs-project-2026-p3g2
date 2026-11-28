package pt.marazul.identity.application;

import org.junit.jupiter.api.Test;
import pt.marazul.cucumber.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginThrottleTest {
    private static final Instant START = Instant.parse("2026-12-01T10:00:00Z");

    private final MutableClock clock = new MutableClock(START, ZoneOffset.UTC);
    private final LoginThrottle throttle = new LoginThrottle(clock);

    private void failAt(String origin, int... secondsFromStart) {
        for (int seconds : secondsFromStart) {
            clock.set(START.plusSeconds(seconds));
            throttle.recordFailure(origin);
        }
    }

    @Test
    void fiveFailuresInAMinuteBlockTheSixthAttempt() {
        failAt("a", 0, 10, 20, 30, 40);
        clock.set(START.plusSeconds(50));

        assertThatThrownBy(() -> throttle.checkAllowed("a"))
                .isInstanceOfSatisfying(TooManyLoginAttempts.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(10)));
    }

    @Test
    void fourFailuresDoNotBlock() {
        failAt("a", 0, 10, 20, 30);
        clock.set(START.plusSeconds(40));

        assertThatCode(() -> throttle.checkAllowed("a")).doesNotThrowAnyException();
    }

    @Test
    void theBlockLiftsWhenTheOldestFailureLeavesTheWindow() {
        failAt("a", 0, 10, 20, 30, 40);
        clock.set(START.plusSeconds(60).plusMillis(1));

        assertThatCode(() -> throttle.checkAllowed("a")).doesNotThrowAnyException();
    }

    @Test
    void retryAfterIsAtLeastOneSecond() {
        failAt("a", 0, 10, 20, 30, 40);
        clock.set(START.plusSeconds(59).plusMillis(900));

        assertThatThrownBy(() -> throttle.checkAllowed("a"))
                .isInstanceOfSatisfying(TooManyLoginAttempts.class,
                        e -> assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(1)));
    }

    @Test
    void originsAreIndependent() {
        failAt("a", 0, 1, 2, 3, 4);
        clock.set(START.plusSeconds(5));

        assertThatCode(() -> throttle.checkAllowed("b")).doesNotThrowAnyException();
        assertThatThrownBy(() -> throttle.checkAllowed("a")).isInstanceOf(TooManyLoginAttempts.class);
    }

    @Test
    void checkingAnOriginWithoutFailuresLeavesNoEntry() {
        throttle.checkAllowed("clean");

        assertThat(throttle.trackedOrigins()).isZero();
    }

    @Test
    void anEntryIsRemovedOnceItsFailuresLeaveTheWindow() {
        failAt("a", 0);
        assertThat(throttle.trackedOrigins()).isEqualTo(1);
        clock.set(START.plusSeconds(61));

        throttle.checkAllowed("a");

        assertThat(throttle.trackedOrigins()).isZero();
    }
}
