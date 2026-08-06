package com.telcoedge.charging;


import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.telcoedge.charging.persistence.TariffRateView;
import com.telcoedge.charging.web.CdrRequest;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.UsageType;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
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

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    TariffRatesLookupService tariffRatesLookupService;
    @Autowired
    private ChargingService chargingService;

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

    @BeforeEach
    void clearTariffCache(){
        var cache = cacheManager.getCache("tariffRates");
        if(cache != null) cache.clear();
    }

    private CacheStats currentStats(){
        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("tariffRates");
        return caffeineCache.getNativeCache().stats();
    }

    private CdrRequest createCdr(){
        return new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());
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

        CdrRequest request = createCdr();
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/charging/cdr", HttpMethod.POST,
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

    @Test
    void RetryConfiguredForSubscriberLookup(){
        Retry retry = retryRegistry.retry("subscriberLookup");
        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        assertThat(retry.getRetryConfig().getIntervalBiFunction()).isNotNull();
    }

    @Test
    void tariffRateIsCachedAfterFirstLookup(){
        UsageType usageType = UsageType.VOICE;
        Optional<TariffRateView> tariffRateView1 = tariffRatesLookupService.findRate(1L, usageType);
        assertThat(tariffRateView1).isPresent();
        assertThat(tariffRateView1.get().getRatePerUnit()).isEqualByComparingTo("0.01");

        Optional<TariffRateView> tariffRateView2 = tariffRatesLookupService.findRate(1L, usageType);
        assertThat(tariffRateView2).isPresent();

        var cache = cacheManager.getCache("tariffRates");
        assertThat(cache).isNotNull();
        assertThat(cache.get("1-VOICE")).isNotNull();

    }

    @Test
    void cacheMetricsAreExposed(){
        tariffRatesLookupService.findRate(1L, UsageType.DATA);
        tariffRatesLookupService.findRate(1L, UsageType.DATA);

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getBody()).contains("cache_gets_total");
        assertThat(response.getBody()).contains("cache=\"tariffRates\"");
    }

    @Test
    void recordsOneMissAndTwoHitsForSameKey(){
        CacheStats before = currentStats();

        Optional<TariffRateView> tariffRateView1 = tariffRatesLookupService.findRate(1L, UsageType.VOICE);
        Optional<TariffRateView> tariffRateView2 = tariffRatesLookupService.findRate(1L, UsageType.VOICE);
        Optional<TariffRateView> tariffRateView3 = tariffRatesLookupService.findRate(1L, UsageType.VOICE);

        assertThat(tariffRateView1).isPresent();
        assertThat(tariffRateView2).isPresent();
        assertThat(tariffRateView3).isPresent();

        assertThat(tariffRateView1.get().getRatePerUnit()).isEqualByComparingTo("0.01");
        assertThat(tariffRateView2.get().getRatePerUnit()).isEqualByComparingTo("0.01");
        assertThat(tariffRateView3.get().getRatePerUnit()).isEqualByComparingTo("0.01");

        CacheStats after = currentStats();
        assertThat(after.missCount() - before.missCount()).isEqualTo(1);
        assertThat(after.hitCount()-before.hitCount()).isEqualTo(2);
    }

    @Test
    void lookupServiceRecordSeparateMissPerUsageType(){
        CacheStats before = currentStats();

        tariffRatesLookupService.findRate(1L, UsageType.VOICE);
        tariffRatesLookupService.findRate(1L, UsageType.VOICE);

        tariffRatesLookupService.findRate(1L, UsageType.DATA);
        tariffRatesLookupService.findRate(1L, UsageType.DATA);

        CacheStats after = currentStats();

        assertThat(after.missCount() - before.missCount()).isEqualTo(2);
        assertThat(after.hitCount()-before.hitCount()).isEqualTo(2);
    }

    @Test
    void chargingServiceSecondCdrHitsTariffCache(){
        CacheStats before = currentStats();
        CdrRequest request1 = createCdr();
        CdrRequest request2 = createCdr();
        ResponseEntity<String> firstResponse = restTemplate.postForEntity("/api/v1/charging/cdr",
                request1, String.class);

        ResponseEntity<String> secondResponse = restTemplate.postForEntity("/api/v1/charging/cdr",
                request2, String.class);

        assertThat(firstResponse.getBody()).contains("CHARGED");
        assertThat(secondResponse.getBody()).contains("CHARGED");

        CacheStats after = currentStats();
        assertThat(after.hitCount()-before.hitCount()).isEqualTo(1);
        assertThat(after.missCount()-before.missCount()).isEqualTo(1);
    }

    //Test Rate limiter with reducing max limit per period in application.yaml
    /*@Test
    void rateLimiterRejects101stRequestIn1Second(){
        for(int i=0;i<101;i++){
            restTemplate.postForEntity("/api/v1/charging/cdr",
                    createCdr(), String.class);
        }

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/charging/cdr",
                createCdr(), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(429);

    }*/



}
