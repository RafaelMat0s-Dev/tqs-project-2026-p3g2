package pt.marazul.identity.infrastructure;

import org.springframework.stereotype.Component;
import pt.marazul.identity.application.CodeHasher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class Sha256CodeHasher implements CodeHasher {
    @Override
    public String hash(String code) {
        String normalized = code.strip().toUpperCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
