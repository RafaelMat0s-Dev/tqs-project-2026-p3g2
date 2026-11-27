package pt.marazul;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.marazul.shared.infrastructure.ClockConfiguration;
import pt.marazul.support.TestcontainersConfiguration;

import java.time.Clock;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApplicationIT {
    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void readinessProbeIsUp() {
        given().get("/actuator/health/readiness")
                .then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void flywayBaselineEnablesBtreeGist() {
        Integer extensions = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Integer.class);
        assertThat(extensions).isEqualTo(1);
    }

    @Test
    void openApiDocumentListsTheApi() {
        given().get("/v3/api-docs")
                .then().statusCode(200).body("paths", hasKey("/api/bookings"));
    }

    @Test
    void prometheusMetricsAreExposed() {
        given().get("/actuator/prometheus")
                .then().statusCode(200).body(containsString("jvm_memory_used_bytes"));
    }

    @Test
    void clockRunsInTheOperatingZone() {
        assertThat(clock.getZone()).isEqualTo(ClockConfiguration.OPERATING_ZONE);
    }
}
