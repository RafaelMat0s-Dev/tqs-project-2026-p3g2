package pt.marazul.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    @Bean
    GenericContainer<?> mosquittoContainer() {
        return MosquittoContainers.create();
    }

    @Bean
    DynamicPropertyRegistrar brokerUrlProperty(GenericContainer<?> mosquittoContainer) {
        return registry -> registry.add("marazul.telemetry.broker-url",
                () -> MosquittoContainers.brokerUrl(mosquittoContainer));
    }
}
