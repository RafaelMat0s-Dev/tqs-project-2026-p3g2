package pt.marazul.telemetry.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.marazul.telemetry.application.TelemetryEvents;
import pt.marazul.telemetry.application.TelemetryIngestionService;
import pt.marazul.telemetry.application.TelemetryMetrics;
import pt.marazul.telemetry.application.TelemetrySource;
import pt.marazul.telemetry.application.VehicleTelemetryRepository;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class TelemetryConfiguration {
    @Bean
    TelemetryIngestionService telemetryIngestionService(VehicleTelemetryRepository repository,
                                                        TelemetryMetrics metrics, TelemetryEvents events,
                                                        Clock clock, TelemetrySource source) {
        var service = new TelemetryIngestionService(repository, metrics, events, clock);
        source.subscribe(service);
        return service;
    }
}
