package pt.marazul.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import pt.marazul.shared.infrastructure.ClockConfiguration;
import pt.marazul.support.TestcontainersConfiguration;

import java.time.Instant;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "marazul.trip.expiry-interval=1h")
@Import({TestcontainersConfiguration.class, CucumberSpringConfiguration.TestClock.class})
public class CucumberSpringConfiguration {
    @TestConfiguration(proxyBeanMethods = false)
    static class TestClock {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now(), ClockConfiguration.OPERATING_ZONE);
        }
    }
}
