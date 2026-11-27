package pt.marazul.identity.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.marazul.identity.domain.StaffAccount;
import pt.marazul.identity.domain.StaffRole;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JwtService {
    private static final String HMAC = "HmacSHA256";
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);
    private final byte[] secret;

    public JwtService(@Value("${marazul.security.jwt-secret:}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
        } else {
            secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String issue(StaffAccount account) {
        long expiresAt = Instant.now().plus(TOKEN_LIFETIME).getEpochSecond();
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"" + account.id() + "\",\"email\":\""
                + escape(account.email()) + "\",\"role\":\"" + account.role() + "\",\"exp\":" + expiresAt + "}");
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    public Optional<Claims> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !constantTimeEquals(sign(parts[0] + "." + parts[1]), parts[2])) {
                return Optional.empty();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long expiry = Long.parseLong(value(payload, "exp"));
            if (expiry <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new Claims(value(payload, "sub"), value(payload, "email"),
                    StaffRole.valueOf(value(payload, "role"))));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar o token.", exception);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String value(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker) + marker.length();
        if (start < marker.length()) {
            throw new IllegalArgumentException("Claim ausente");
        }
        if (json.charAt(start) == '\"') {
            int end = json.indexOf('\"', start + 1);
            return json.substring(start + 1, end);
        }
        int end = json.indexOf(',', start);
        return json.substring(start, end < 0 ? json.length() : end).replace("}", "");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    public record Claims(String subject, String email, StaffRole role) { }
}
