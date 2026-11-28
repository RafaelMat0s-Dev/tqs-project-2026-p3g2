package pt.marazul.identity.application;

import java.util.Optional;

public interface TokenIssuer {
    String issue(GuestSession session);

    Optional<GuestSession> verify(String token);
}
