package com.telcoedge.charging;


import com.telcoedge.charging.persistence.BalanceRepository;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class ChargingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ChargingService chargingService;

    @Autowired
    BalanceRepository balanceRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    void shouldChargeSuccessfully(){
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        ChargeResult result = chargingService.process(cdr);

        assertThat(result.status()).isEqualTo(ChargeStatus.CHARGED);
        assertThat(result.amountCharged().compareTo(new BigDecimal("0.60"))).isZero();
        assertThat(result.remainingBalance().compareTo(new BigDecimal("999.40"))).isZero();
    }

    @Test
    void shouldRejectDuplicate(){
        UUID eventId = UUID.randomUUID();
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        chargingService.process(cdr);
        ChargeResult duplicate = chargingService.process(cdr);
        assertThat(duplicate.status()).isEqualTo(ChargeStatus.DUPLICATE);

    }

    @Test
    void shouldRejectInsufficientBalance(){
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.DATA, new BigDecimal("3000"), Instant.now(), Instant.now());
        ChargeResult result = chargingService.process(cdr);
        assertThat(result.status()).isEqualTo(ChargeStatus.INSUFFICIENT_BALANCE);
    }

    @Test
    void shouldResultInSubscriberNotFound(){
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "0000000000",
                UsageType.DATA, new BigDecimal("3000"), Instant.now(), Instant.now());
        ChargeResult result = chargingService.process(cdr);
        assertThat(result.status()).isEqualTo(ChargeStatus.SUBSCRIBER_NOT_FOUND);
    }

    @Test
    void ChargingPathShouldNotExceedExpectedQueryCount(){
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.DATA, new BigDecimal("3000"), Instant.now(), Instant.now());

        chargingService.process(cdr);

        long totalStatements = stats.getPrepareStatementCount();
        assertThat(totalStatements)
                .as("Expected <=8 SQL statements from a single charge (got %d)", totalStatements)
                .isLessThanOrEqualTo(8);

        stats.setStatisticsEnabled(false);
    }
}
