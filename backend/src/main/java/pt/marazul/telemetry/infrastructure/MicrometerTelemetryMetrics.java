package pt.marazul.telemetry.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import pt.marazul.telemetry.application.TelemetryMetrics;

import java.time.Duration;

@Component
class MicrometerTelemetryMetrics implements TelemetryMetrics {
    private final Timer delay;
    private final Counter discarded;
    private final Counter rejected;

    MicrometerTelemetryMetrics(MeterRegistry registry) {
        this.delay = Timer.builder("marazul.telemetry.delay")
                .publishPercentileHistogram()
                .serviceLevelObjectives(Duration.ofSeconds(5))
                .register(registry);
        this.discarded = Counter.builder("marazul.telemetry.discarded").register(registry);
        this.rejected = Counter.builder("marazul.telemetry.rejected").register(registry);
    }

    @Override
    public void readingApplied(Duration delay) {
        this.delay.record(delay);
    }

    @Override
    public void readingDiscarded() {
        discarded.increment();
    }

    @Override
    public void readingRejected() {
        rejected.increment();
    }
}
