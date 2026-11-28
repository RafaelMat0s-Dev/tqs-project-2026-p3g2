package pt.marazul.identity.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.GuestAccessWindow;
import pt.marazul.identity.domain.Stay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class GuestLoginService {
    private final GuestDirectory directory;
    private final CodeHasher hasher;
    private final TokenIssuer tokens;
    private final LoginThrottle throttle;
    private final Clock clock;
    private final Duration maxSession;

    public GuestLoginService(GuestDirectory directory, CodeHasher hasher, TokenIssuer tokens, LoginThrottle throttle,
                             Clock clock, @Value("${marazul.identity.max-session:8h}") Duration maxSession) {
        this.directory = directory;
        this.hasher = hasher;
        this.tokens = tokens;
        this.throttle = throttle;
        this.clock = clock;
        this.maxSession = maxSession;
    }

    public LoginResult login(String code, String origin) {
        throttle.checkAllowed(origin);
        Optional<Guest> found = directory.findByCodeHash(hasher.hash(code));
        Optional<Stay> stay = found.flatMap(guest -> directory.findStay(guest.stayId()));
        if (found.isEmpty() || stay.isEmpty()) {
            throttle.recordFailure(origin);
            throw new InvalidGuestCode();
        }
        Instant now = clock.instant();
        GuestAccessWindow window = stay.get().accessWindow();
        if (!window.allowsLoginAt(now)) {
            throw window.closed();
        }
        Guest guest = found.get();
        GuestSession session = new GuestSession(guest.id(), guest.name(), window.sessionExpiry(now, maxSession));
        return new LoginResult(tokens.issue(session), session);
    }

    public Optional<GuestSession> session(String token) {
        return tokens.verify(token);
    }
}
