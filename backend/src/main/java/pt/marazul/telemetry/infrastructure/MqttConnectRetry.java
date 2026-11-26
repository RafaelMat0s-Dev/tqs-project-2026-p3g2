package pt.marazul.telemetry.infrastructure;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

// esqueleto para os testes compilarem: a lógica vem com a implementação
final class MqttConnectRetry {
    MqttConnectRetry(Callable<?> connect, ScheduledExecutorService scheduler, Duration interval) {
        throw new UnsupportedOperationException();
    }

    void start() {
        throw new UnsupportedOperationException();
    }

    void onConnected() {
        throw new UnsupportedOperationException();
    }

    void onConnectFailed() {
        throw new UnsupportedOperationException();
    }

    void stop() {
        throw new UnsupportedOperationException();
    }
}
