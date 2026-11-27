package pt.marazul.telemetry.infrastructure;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pt.marazul.telemetry.api.TelemetryReadingReceived;
import pt.marazul.telemetry.application.TelemetryEvents;
import pt.marazul.telemetry.domain.TelemetryReading;

@Component
class SpringTelemetryEvents implements TelemetryEvents {
    private final ApplicationEventPublisher publisher;

    SpringTelemetryEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void readingApplied(TelemetryReading reading) {
        publisher.publishEvent(new TelemetryReadingReceived(reading.vehicleCode(), reading.originAt(),
                reading.batteryPercentage(), reading.locked(), reading.hubId(), reading.shock()));
    }
}
