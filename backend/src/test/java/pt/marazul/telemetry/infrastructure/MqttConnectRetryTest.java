package pt.marazul.telemetry.infrastructure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MqttConnectRetryTest {
    private static final Duration INTERVAL = Duration.ofSeconds(2);

    /** Agendador determinístico: guarda as tarefas e só as corre quando o teste manda. */
    static class ManualScheduler extends ScheduledThreadPoolExecutor {
        final List<Runnable> pending = new ArrayList<>();
        final List<Long> delaysMillis = new ArrayList<>();

        ManualScheduler() {
            super(0);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            pending.add(command);
            delaysMillis.add(unit.toMillis(delay));
            return super.schedule(() -> { }, 1, TimeUnit.DAYS);
        }

        void runNext() {
            pending.remove(0).run();
        }
    }

    private final ManualScheduler scheduler = new ManualScheduler();
    private int attempts;
    private int failuresLeft;
    private final MqttConnectRetry retry = new MqttConnectRetry(() -> {
        attempts++;
        if (failuresLeft-- > 0) {
            throw new IOException("broker indisponível");
        }
        return null;
    }, scheduler, INTERVAL);

    @Test
    void firstAttemptIsMadeImmediatelyOnStart() {
        retry.start();

        assertThat(attempts).isEqualTo(1);
        assertThat(scheduler.pending).isEmpty();
    }

    @Test
    void retriesEveryIntervalAndConnectsOnTheThirdAttempt() {
        failuresLeft = 2;

        retry.start();
        assertThat(attempts).isEqualTo(1);
        assertThat(scheduler.pending).hasSize(1);
        scheduler.runNext();
        assertThat(attempts).isEqualTo(2);
        assertThat(scheduler.pending).hasSize(1);
        scheduler.runNext();

        assertThat(attempts).isEqualTo(3);
        assertThat(scheduler.pending).isEmpty();
        assertThat(scheduler.delaysMillis).containsExactly(2000L, 2000L);
    }

    @Test
    void asynchronousConnectionFailureSchedulesAnotherAttempt() {
        retry.start();

        retry.onConnectFailed();
        assertThat(scheduler.pending).hasSize(1);
        scheduler.runNext();

        assertThat(attempts).isEqualTo(2);
    }

    @Test
    void stopsRetryingOnceConnected() {
        failuresLeft = 1;
        retry.start();
        scheduler.runNext();

        retry.onConnected();
        retry.onConnectFailed();

        assertThat(attempts).isEqualTo(2);
        assertThat(scheduler.pending).isEmpty();
    }

    @Test
    void stopCancelsPendingAttempts() {
        failuresLeft = 5;
        retry.start();
        assertThat(scheduler.pending).hasSize(1);

        retry.stop();
        scheduler.pending.forEach(Runnable::run);

        assertThat(attempts).isEqualTo(1);
        assertThat(scheduler.pending).hasSize(1);
    }
}
