package pt.marazul.telemetry.infrastructure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerTelemetryMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerTelemetryMetrics metrics = new MicrometerTelemetryMetrics(registry);

    @Test
    void appliedReadingRecordsItsDelayInTheTimer() {
        metrics.readingApplied(Duration.ofSeconds(3));

        var timer = registry.get("marazul.telemetry.delay").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isEqualTo(3.0);
    }

    @Test
    void discardedReadingIncrementsTheDiscardedCounter() {
        metrics.readingDiscarded();

        assertThat(registry.get("marazul.telemetry.discarded").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("marazul.telemetry.rejected").counter().count()).isZero();
    }

    @Test
    void rejectedMessageIncrementsTheRejectedCounter() {
        metrics.readingRejected();

        assertThat(registry.get("marazul.telemetry.rejected").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("marazul.telemetry.discarded").counter().count()).isZero();
    }
}
