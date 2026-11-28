package pt.marazul.identity.infrastructure;

import org.junit.jupiter.api.Test;
import pt.marazul.cucumber.MutableClock;
import pt.marazul.identity.application.GuestSession;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes-ok";
    private final MutableClock clock = new MutableClock(Instant.parse("2026-12-01T12:50:00Z"), ZoneOffset.UTC);
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(SECRET, clock);
    private final GuestSession session =
            new GuestSession(UUID.randomUUID(), "Marta", Instant.parse("2026-12-01T13:00:00Z"));

    @Test
    void roundTripKeepsTheSession() {
        assertThat(issuer.verify(issuer.issue(session))).contains(session);
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        String foreign = new JwtTokenIssuer("another-secret-with-at-least-32-bytes", clock).issue(session);

        assertThat(issuer.verify(foreign)).isEmpty();
    }

    @Test
    void withoutASecretEachStartUsesARandomOne() {
        JwtTokenIssuer first = new JwtTokenIssuer("", clock);
        JwtTokenIssuer second = new JwtTokenIssuer(null, clock);

        assertThat(first.verify(first.issue(session))).contains(session);
        assertThat(second.verify(first.issue(session))).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(issuer.verify("abc")).isEmpty();
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = issuer.issue(session);
        clock.set(Instant.parse("2026-12-01T13:00:00Z"));

        assertThat(issuer.verify(token)).isEmpty();
    }

    @Test
    void aTokenWithoutTheExpectedClaimsIsRejected() throws Exception {
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder().subject("x").build();
        var jwt = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        jwt.sign(new com.nimbusds.jose.crypto.MACSigner(SECRET.getBytes()));

        assertThat(issuer.verify(jwt.serialize())).isEmpty();
    }

    @Test
    void aShortSecretIsRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtTokenIssuer("short", clock)).isInstanceOf(IllegalArgumentException.class);
    }
}
