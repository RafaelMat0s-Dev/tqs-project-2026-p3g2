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

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
class MqttTelemetryAdapter implements TelemetrySource, SmartLifecycle, MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryAdapter.class);
    private static final int QOS = 1;
    private static final int RETRY_SECONDS = 2;

    private final String brokerUrl;
    private final String topicFilter;
    private final String clientId;
    private final TelemetryMessageParser parser;
    private final ScheduledExecutorService retries = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "mqtt-telemetry-connect");
        thread.setDaemon(true);
        return thread;
    });

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
        connect();
    }

    @Override
    public void stop() {
        running = false;
        retries.shutdownNow();
        try {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion(1000);
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Falha ao fechar a ligação MQTT", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void connect() {
        var options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(5);
        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    log.info("Ligado ao broker MQTT {}", brokerUrl);
                }

                @Override
                public void onFailure(IMqttToken token, Throwable exception) {
                    log.warn("Broker MQTT {} indisponível; nova tentativa em {} s", brokerUrl, RETRY_SECONDS);
                    scheduleRetry();
                }
            });
        } catch (MqttException e) {
            log.warn("Falha ao iniciar a ligação MQTT; nova tentativa em {} s", RETRY_SECONDS, e);
            scheduleRetry();
        }
    }

    private void scheduleRetry() {
        if (running && !retries.isShutdown()) {
            retries.schedule(this::connect, RETRY_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        try {
            client.subscribe(topicFilter, QOS);
            log.info("Subscrito {} (reconexão: {})", topicFilter, reconnect);
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
            log.warn("Mensagem de telemetria rejeitada em {}: {}", topic, e.getMessage());
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
