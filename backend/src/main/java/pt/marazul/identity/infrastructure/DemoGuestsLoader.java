package pt.marazul.identity.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.marazul.identity.application.CodeHasher;
import pt.marazul.identity.application.GuestDirectory;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.Stay;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Profile("demo")
public class DemoGuestsLoader implements ApplicationRunner {
    private final GuestDirectory directory;
    private final CodeHasher hasher;
    private final Clock clock;

    public DemoGuestsLoader(GuestDirectory directory, CodeHasher hasher, Clock clock) {
        this.directory = directory;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        Stay stay = new Stay(UUID.randomUUID(), "demo-stay", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(5)));
        directory.save(stay);
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Marta", hasher.hash("MARTA-2026")));
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Pedro", hasher.hash("PEDRO-2026")));
    }
}
