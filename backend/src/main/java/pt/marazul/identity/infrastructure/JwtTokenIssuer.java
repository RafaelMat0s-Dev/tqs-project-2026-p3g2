package pt.marazul.identity.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pt.marazul.identity.application.GuestSession;
import pt.marazul.identity.application.TokenIssuer;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenIssuer implements TokenIssuer {
    private static final int MIN_SECRET_BYTES = 32;

    private final byte[] secret;
    private final Clock clock;

    public JwtTokenIssuer(@Value("${marazul.identity.jwt-secret}") String secret, Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("marazul.identity.jwt-secret must have at least 32 bytes");
        }
        this.secret = bytes;
        this.clock = clock;
    }

    @Override
    public String issue(GuestSession session) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(session.guestId().toString())
                .claim("name", session.name())
                .expirationTime(Date.from(session.expiresAt()))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign the session token", e);
        }
        return jwt.serialize();
    }

    @Override
    public Optional<GuestSession> verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            String name = claims.getStringClaim("name");
            if (expiration == null || name == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            Instant expiresAt = expiration.toInstant();
            if (!expiresAt.isAfter(clock.instant())) {
                return Optional.empty();
            }
            return Optional.of(new GuestSession(UUID.fromString(claims.getSubject()), name, expiresAt));
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
