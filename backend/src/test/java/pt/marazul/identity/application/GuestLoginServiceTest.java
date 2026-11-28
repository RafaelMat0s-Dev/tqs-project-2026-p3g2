package pt.marazul.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.marazul.cucumber.MutableClock;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.IdentityRuleViolation;
import pt.marazul.identity.domain.Stay;
import pt.marazul.identity.infrastructure.InMemoryGuestDirectory;
import pt.marazul.identity.infrastructure.JwtTokenIssuer;
import pt.marazul.identity.infrastructure.Sha256CodeHasher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuestLoginServiceTest {
    private static final Instant CHECK_IN = Instant.parse("2026-11-24T15:00:00Z");
    private static final Instant CHECK_OUT = Instant.parse("2026-12-01T11:00:00Z");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-01T12:50:00Z"), ZoneOffset.UTC);
    private final InMemoryGuestDirectory directory = new InMemoryGuestDirectory();
    private final Sha256CodeHasher hasher = new Sha256CodeHasher();
    private final JwtTokenIssuer tokens = new JwtTokenIssuer("test-secret-with-at-least-32-bytes-ok", clock);
    private final GuestLoginService service =
            new GuestLoginService(directory, hasher, tokens, new LoginThrottle(clock), clock, Duration.ofHours(8));

    @BeforeEach
    void guestSetup() {
        Stay stay = new Stay(UUID.randomUUID(), "ref", CHECK_IN, CHECK_OUT);
        directory.save(stay);
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Marta", hasher.hash("MARTA-2026")));
    }

    @Test
    void loginAtTheLastMinuteOfTheWindowIssuesASessionUntilTheWindowCloses() {
        LoginResult result = service.login("MARTA-2026", "o1");

        assertThat(result.session().expiresAt()).isEqualTo(Instant.parse("2026-12-01T13:00:00Z"));
        assertThat(result.session().name()).isEqualTo("Marta");
        assertThat(result.token()).isNotBlank();
    }

    @Test
    void loginOneMinuteAfterTheWindowIsBr13() {
        clock.set(Instant.parse("2026-12-01T13:01:00Z"));

        assertThatThrownBy(() -> service.login("MARTA-2026", "o1"))
                .isInstanceOfSatisfying(IdentityRuleViolation.class, e -> assertThat(e.rule()).isEqualTo("BR13"));
    }

    @Test
    void loginBeforeCheckInIsBr13() {
        clock.set(CHECK_IN.minusSeconds(1));

        assertThatThrownBy(() -> service.login("MARTA-2026", "o1"))
                .isInstanceOfSatisfying(IdentityRuleViolation.class, e -> assertThat(e.rule()).isEqualTo("BR13"));
    }

    @Test
    void aWrongCodeIsInvalidAndCountsAsAFailure() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login("ERRADO", "o1")).isInstanceOf(InvalidGuestCode.class);
        }

        assertThatThrownBy(() -> service.login("ERRADO", "o1")).isInstanceOf(TooManyLoginAttempts.class);
    }

    @Test
    void aGuestWithoutAStayIsInvalid() {
        directory.save(new Guest(UUID.randomUUID(), UUID.randomUUID(), "Orfao", hasher.hash("ORFAO")));

        assertThatThrownBy(() -> service.login("ORFAO", "o1")).isInstanceOf(InvalidGuestCode.class);
    }

    @Test
    void aBr13RefusalDoesNotCountAsAFailure() {
        clock.set(Instant.parse("2026-12-01T13:01:00Z"));
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login("MARTA-2026", "o1")).isInstanceOf(IdentityRuleViolation.class);
        }
        clock.set(Instant.parse("2026-12-01T12:50:00Z"));

        assertThatCode(() -> service.login("MARTA-2026", "o1")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginDoesNotCountAsAFailure() {
        for (int i = 0; i < 6; i++) {
            assertThatCode(() -> service.login("MARTA-2026", "o1")).doesNotThrowAnyException();
        }
    }

    @Test
    void codeIsNormalized() {
        assertThatCode(() -> service.login(" marta-2026 ", "o1")).doesNotThrowAnyException();
    }

    @Test
    void sessionIsRejectedOnceExpired() {
        String token = service.login("MARTA-2026", "o1").token();
        assertThat(service.session(token)).isPresent();

        clock.set(Instant.parse("2026-12-01T13:00:01Z"));

        assertThat(service.session(token)).isEmpty();
    }
}
