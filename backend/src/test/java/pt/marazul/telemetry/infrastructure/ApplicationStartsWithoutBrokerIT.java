package pt.marazul.telemetry.infrastructure;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.postgresql.PostgreSQLContainer;
import pt.marazul.support.TestcontainersConfiguration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

// Not importing TestcontainersConfiguration: it registers the Mosquitto broker URL; here nobody listens.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "marazul.telemetry.broker-url=tcp://localhost:1")
@Import(ApplicationStartsWithoutBrokerIT.PostgresOnly.class)
class ApplicationStartsWithoutBrokerIT {
    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresOnly {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE);
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    Environment environment;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void applicationStartsAndIsReadyWhenBrokerIsUnreachable() {
        assertThat(environment.getProperty("marazul.telemetry.broker-url")).isEqualTo("tcp://localhost:1");

        given().when().get("/actuator/health/readiness")
                .then().statusCode(200).body("status", equalTo("UP"));
    }
}
