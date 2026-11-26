package pt.marazul.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class MosquittoContainers {
    public static final DockerImageName IMAGE = DockerImageName.parse("eclipse-mosquitto:2");
    public static final int MQTT_PORT = 1883;

    private MosquittoContainers() {
    }

    @SuppressWarnings("resource")
    public static GenericContainer<?> create() {
        return new GenericContainer<>(IMAGE)
                .withExposedPorts(MQTT_PORT)
                .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
                .waitingFor(Wait.forLogMessage(".*mosquitto version .* running.*", 1));
    }

    public static String brokerUrl(GenericContainer<?> broker) {
        return "tcp://" + broker.getHost() + ":" + broker.getMappedPort(MQTT_PORT);
    }
}
