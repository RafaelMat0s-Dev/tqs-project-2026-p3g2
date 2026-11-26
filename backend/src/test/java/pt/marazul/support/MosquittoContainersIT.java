package pt.marazul.support;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MosquittoContainersIT {
    @Container
    static final GenericContainer<?> BROKER = MosquittoContainers.create();

    @Test
    void brokerIsReachable() {
        assertThat(BROKER.isRunning()).isTrue();
        assertThat(MosquittoContainers.brokerUrl(BROKER)).matches("tcp://.+:\\d+");
    }
}
