package com.telcoedge.charging;


import com.telcoedge.charging.web.CdrRequest;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=", // Fixes 404 isolation routing
                "management.prometheus.metrics.export.enabled=true", // Activates registry bean
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus" // Ensures exposure exposure
        }
)
@Testcontainers
public class ChargingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedTestData(){
        jdbcTemplate.execute("DELETE FROM idempotency_keys");
        jdbcTemplate.execute("DELETE FROM usage_events");
        jdbcTemplate.execute("DELETE FROM balances");
        jdbcTemplate.execute("DELETE FROM subscriber_plans");
        jdbcTemplate.execute("DELETE FROM subscribers WHERE msisdn='9876543000'");

        jdbcTemplate.update("""
                INSERT INTO subscribers(operator_id, msisdn, name , status)
                VALUES('acme', '9876543000','Test User', 'ACTIVE');
                """);

        Long subscriberId = jdbcTemplate.queryForObject("SELECT id from subscribers" +
                " where msisdn = '9876543000'", Long.class);

        jdbcTemplate.update("""
                INSERT INTO subscriber_plans(subscriber_id, plan_id, active)
                VALUES(?,1,true);
                """, subscriberId);

        jdbcTemplate.update("""
                INSERT INTO balances(subscriber_id, amount, version)
                VALUES(?, '1000.0000',0);
                """, subscriberId);
    }


    @Test
    void responseIncludesCorrelationId(){
        CdrRequest request = new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/charging/cdr",
                request, String.class);

        String correlationId = response.getHeaders().getFirst("X-Correlation-Id");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).hasSize(8);
    }

    @Test
    void callerProvidedCorrelationIdIsPreserved(){

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", "test-1234");

        CdrRequest request = new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/charging/", HttpMethod.POST,
                new HttpEntity<>( request, headers), String.class);

        assertThat( response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("test-1234");

    }

    @Test
    void readinessEndpointReportUp(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void livenessEndpointReportUp(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health/liveness", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void readinessReportsDownWhenNegativeBalanceExist(){
        Long subscriberId = jdbcTemplate.queryForObject("SELECT id from subscribers" +
                " where msisdn = '9876543000'", Long.class);
        jdbcTemplate.update("""
                update balances set amount =-10.00 where subscriber_id=?;
                """, subscriberId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health/readiness", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).contains("DOWN");

        jdbcTemplate.update(
                "delete from balances where subscriber_id=?", subscriberId);
    }
}
