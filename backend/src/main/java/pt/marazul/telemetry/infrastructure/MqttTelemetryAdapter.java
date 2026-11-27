package pt.marazul.telemetry.infrastructure;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import pt.marazul.telemetry.application.TelemetryListener;
import pt.marazul.telemetry.application.TelemetrySource;
import pt.marazul.telemetry.domain.InvalidTelemetryReading;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
class MqttTelemetryAdapter implements TelemetrySource, SmartLifecycle, MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);
    private static final int QOS = 1;
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(2);

    private final String brokerUrl;
    private final String topicFilter;
    private final String clientId;
    private final TelemetryMessageParser parser;
    private final ScheduledExecutorService retries = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "mqtt-telemetry-connect");
        thread.setDaemon(true);
        return thread;
    });

    private volatile MqttConnectRetry retry;
    private volatile TelemetryListener listener;
    private volatile MqttAsyncClient client;
    private volatile boolean running;

    MqttTelemetryAdapter(@Value("${marazul.telemetry.broker-url}") String brokerUrl,
                         @Value("${marazul.telemetry.topic-filter:marazul/vehicles/+/telemetry}") String topicFilter,
                         @Value("${marazul.telemetry.client-id:marazul-backend-}") String clientIdPrefix,
                         TelemetryMessageParser parser) {
        this.brokerUrl = brokerUrl;
        this.topicFilter = topicFilter;
        this.clientId = clientIdPrefix + UUID.randomUUID();
        this.parser = parser;
    }

    @Override
    public void subscribe(TelemetryListener listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        try {
            client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(this);
        } catch (MqttException e) {
            log.error("Cliente MQTT inválido para {}", brokerUrl, e);
            return;
        }
        running = true;
        retry = new MqttConnectRetry(this::connect, retries, RETRY_INTERVAL);
        retry.start();
    }

    @Override
    public void stop() {
        running = false;
        if (retry != null) {
            retry.stop();
        }
        retries.shutdownNow();
        var current = client;
        if (current == null) {
            return;
        }
        try {
            current.disconnectForcibly(0, 1);
        } catch (MqttException e) {
            log.warn("Falha ao terminar a ligação MQTT", e);
        }
        try {
            current.close(true);
        } catch (MqttException e) {
            log.warn("Falha ao fechar o cliente MQTT", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private Object connect() throws MqttException {
        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(5);
        return client.connect(options, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                retry.onConnected();
                log.info("Ligado ao broker MQTT {}", brokerUrl);
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                log.warn("Broker MQTT {} indisponível; nova tentativa em {} s", brokerUrl, RETRY_INTERVAL.toSeconds());
                retry.onConnectFailed();
            }
        });
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        try {
            client.subscribe(topicFilter, QOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    log.info("Subscrito {} (reconexão: {})", topicFilter, reconnect);
                }

                @Override
                public void onFailure(IMqttToken token, Throwable exception) {
                    log.error("Falha ao subscrever {}", topicFilter, exception);
                }
            });
        } catch (MqttException e) {
            log.error("Falha ao subscrever {}", topicFilter, e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Ligação MQTT perdida; reconexão automática", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        var target = listener;
        try {
            target.onReading(parser.parse(topic, message.getPayload()));
        } catch (Exception e) {
            if (e instanceof InvalidTelemetryReading) {
                log.warn("Mensagem de telemetria rejeitada em {}: {}", topic, e.getMessage());
            } else {
                log.error("Erro ao processar telemetria em {}", topic, e);
            }
            try {
                target.onInvalidMessage(String.valueOf(e.getMessage()));
            } catch (Exception inner) {
                log.error("Falha ao registar mensagem rejeitada", inner);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
