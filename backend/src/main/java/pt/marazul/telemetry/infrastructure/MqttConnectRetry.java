package pt.marazul.telemetry.infrastructure;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class MqttConnectRetry {
    private final Callable<?> connect;
    private final ScheduledExecutorService scheduler;
    private final Duration interval;
    private volatile boolean active;

    MqttConnectRetry(Callable<?> connect, ScheduledExecutorService scheduler, Duration interval) {
        this.connect = connect;
        this.scheduler = scheduler;
        this.interval = interval;
    }

    void start() {
        active = true;
        attempt();
    }

    void onConnected() {
        active = false;
    }

    void onConnectFailed() {
        scheduleNext();
    }

    void stop() {
        active = false;
    }

    private void attempt() {
        if (!active) {
            return;
        }
        try {
            connect.call();
        } catch (Exception e) {
            scheduleNext();
        }
    }

    private void scheduleNext() {
        if (!active) {
            return;
        }
        try {
            scheduler.schedule(this::attempt, interval.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            active = false;
        }
    }
}
