package pt.marazul.telemetry.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TelemetryIngestionServiceTest {
    private final Instant now = Instant.parse("2026-11-20T14:31:10Z");
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
        var reading = reading(Instant.parse("2026-11-20T14:31:07Z"));
        when(repository.saveIfNewer(reading)).thenReturn(true);

        service.onReading(reading);

        verify(metrics).readingApplied(Duration.ofSeconds(3));
        verify(events).readingApplied(reading);
        verify(metrics, never()).readingDiscarded();
    }

    @Test
    void discardedReadingIsCountedWithoutEventOrDelay() {
        var reading = reading(Instant.parse("2026-11-20T14:31:05Z"));
        when(repository.saveIfNewer(reading)).thenReturn(false);

        service.onReading(reading);

        verify(metrics).readingDiscarded();
        verify(metrics, never()).readingApplied(any());
        verify(events, never()).readingApplied(any());
    }

    @Test
    void invalidMessageIsCountedAsRejectedAndTouchesNothingElse() {
        service.onInvalidMessage("motivo");

        verify(metrics).readingRejected();
        verifyNoInteractions(repository, events);
    }

    @Test
    void readingFromTheFutureRecordsZeroDelay() {
        var reading = reading(now.plusSeconds(5));
        when(repository.saveIfNewer(reading)).thenReturn(true);

        service.onReading(reading);

        verify(metrics).readingApplied(Duration.ZERO);
    }

    @ParameterizedTest(name = "originAt {0} s à frente do relógio é aplicada")
    @ValueSource(longs = {0, 1, 60})
    void readingUpToOneMinuteAheadOfTheClockIsApplied(long secondsAhead) {
        var reading = reading(now.plusSeconds(secondsAhead));
        when(repository.saveIfNewer(reading)).thenReturn(true);

        service.onReading(reading);

        verify(repository).saveIfNewer(reading);
        verify(events).readingApplied(reading);
    }

    @ParameterizedTest(name = "originAt {0} s à frente do relógio é rejeitada")
    @ValueSource(longs = {61, 3600, 86_400})
    void readingMoreThanOneMinuteAheadOfTheClockIsRejected(long secondsAhead) {
        service.onReading(reading(now.plusSeconds(secondsAhead)));

        verify(metrics).readingRejected();
        verify(metrics, never()).readingApplied(any());
        verify(metrics, never()).readingDiscarded();
        verifyNoInteractions(repository, events);
    }

    @Test
    void latestDelegatesToTheRepository() {
        var reading = reading(now);
        when(repository.findLatest("B-17")).thenReturn(Optional.of(reading));

        assertThat(service.latest("B-17")).contains(reading);
    }
}
