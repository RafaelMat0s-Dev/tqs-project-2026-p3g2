package pt.marazul.telemetry.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TelemetryIngestionServiceTest {
    private final Instant now = Instant.parse("2026-11-27T14:31:10Z");
    private final VehicleTelemetryRepository repository = mock(VehicleTelemetryRepository.class);
    private final TelemetryMetrics metrics = mock(TelemetryMetrics.class);
    private final TelemetryEvents events = mock(TelemetryEvents.class);
    private TelemetryIngestionService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryIngestionService(repository, metrics, events, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static TelemetryReading reading(Instant at) {
        return new TelemetryReading("B-17", at, 47, true, "sao-bras", false);
    }

    @Test
    void appliedReadingRecordsDelayAndPublishesEvent() {
        var reading = reading(Instant.parse("2026-11-27T14:31:07Z"));
        when(repository.saveIfNewer(reading)).thenReturn(true);

        service.onReading(reading);

        verify(metrics).readingApplied(Duration.ofSeconds(3));
        verify(events).readingApplied(reading);
        verifyNoMoreInteractions(metrics, events);
    }

    @Test
    void discardedReadingIsCountedWithoutEventOrDelay() {
        var reading = reading(Instant.parse("2026-11-27T14:31:05Z"));
        when(repository.saveIfNewer(reading)).thenReturn(false);

        service.onReading(reading);

        verify(metrics).readingDiscarded();
        verifyNoMoreInteractions(metrics);
        verifyNoInteractions(events);
    }

    @Test
    void invalidMessageIsCountedAsRejectedAndTouchesNothingElse() {
        service.onInvalidMessage("motivo");

        verify(metrics).readingRejected();
        verifyNoMoreInteractions(metrics);
        verifyNoInteractions(repository, events);
    }

    @Test
    void readingFromTheFutureRecordsZeroDelay() {
        var reading = reading(now.plusSeconds(5));
        when(repository.saveIfNewer(reading)).thenReturn(true);

        service.onReading(reading);

        verify(metrics).readingApplied(Duration.ZERO);
    }

    @Test
    void latestDelegatesToTheRepository() {
        var reading = reading(now);
        when(repository.findLatest("B-17")).thenReturn(Optional.of(reading));

        assertThat(service.latest("B-17")).contains(reading);
    }
}
