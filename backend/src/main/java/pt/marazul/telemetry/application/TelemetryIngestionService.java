package pt.marazul.telemetry.application;

import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class TelemetryIngestionService implements TelemetryListener {
    private final VehicleTelemetryRepository repository;
    private final TelemetryMetrics metrics;
    private final TelemetryEvents events;
    private final Clock clock;

    public TelemetryIngestionService(VehicleTelemetryRepository repository, TelemetryMetrics metrics,
                                     TelemetryEvents events, Clock clock) {
        this.repository = repository;
        this.metrics = metrics;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public void onReading(TelemetryReading reading) {
        if (repository.saveIfNewer(reading)) {
            var delay = Duration.between(reading.originAt(), clock.instant());
            metrics.readingApplied(delay.isNegative() ? Duration.ZERO : delay);
            events.readingApplied(reading);
        } else {
            metrics.readingDiscarded();
        }
    }

    @Override
    public void onInvalidMessage(String reason) {
        metrics.readingRejected();
    }

    public Optional<TelemetryReading> latest(String vehicleCode) {
        return repository.findLatest(vehicleCode);
    }
}
