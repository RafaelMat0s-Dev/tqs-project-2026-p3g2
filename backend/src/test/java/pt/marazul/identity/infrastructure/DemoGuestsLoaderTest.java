package pt.marazul.identity.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DemoGuestsLoaderTest {
    @Test
    void loadsMartaAndPedroWithAnOpenStay() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), ZoneOffset.UTC);
        InMemoryGuestDirectory directory = new InMemoryGuestDirectory();
        Sha256CodeHasher hasher = new Sha256CodeHasher();

        new DemoGuestsLoader(directory, hasher, clock).run(null);

        var marta = directory.findByCodeHash(hasher.hash("marta-2026")).orElseThrow();
        assertThat(marta.name()).isEqualTo("Marta");
        assertThat(directory.findByCodeHash(hasher.hash("PEDRO-2026"))).isPresent();
        assertThat(directory.findStay(marta.stayId()).orElseThrow().accessWindow().allowsLoginAt(clock.instant())).isTrue();
    }
}
