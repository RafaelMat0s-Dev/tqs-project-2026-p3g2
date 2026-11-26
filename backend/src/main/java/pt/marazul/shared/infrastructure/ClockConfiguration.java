package pt.marazul.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {
    public static final ZoneId OPERATING_ZONE = ZoneId.of("Europe/Lisbon");

    @Bean
    Clock clock() {
        return Clock.system(OPERATING_ZONE);
    }
}
