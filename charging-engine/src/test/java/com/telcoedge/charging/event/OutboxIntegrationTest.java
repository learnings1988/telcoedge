package com.telcoedge.charging.event;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcoedge.charging.ChargingService;
import com.telcoedge.charging.persistence.OutboxEventEntity;
import com.telcoedge.charging.persistence.OutboxEventRepository;
import com.telcoedge.charging.web.CdrRequest;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
@Testcontainers
public class OutboxIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("telcoedge.outbox.poller.enabled", ()-> false);
    }

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    ChargingService chargingService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OutboxPoller outboxPoller;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    KafkaTemplate<String, CdrEvent> kafkaTemplate;

    @BeforeEach
    void clean(){
        jdbcTemplate.execute("delete from outbox_events");
        jdbcTemplate.execute("delete from balances");
        jdbcTemplate.execute("delete from idempotency_keys");
        jdbcTemplate.execute("delete from usage_events");
        jdbcTemplate.execute("delete from subscriber_plans");
        jdbcTemplate.execute("delete from subscribers where msisdn='9876543000'");

        jdbcTemplate.update("""
                insert into subscribers (operator_id, msisdn, name, status)
                values('acme', '9876543000', 'outbox_test', 'ACTIVE')
                """);

        Long subscriberId = jdbcTemplate.queryForObject(
                "select id from subscribers where msisdn='9876543000'", Long.class);

        jdbcTemplate.update("""
                insert into subscriber_plans(subscriber_id, plan_id, active)
                values(?, 1, true)
                """, subscriberId);

        jdbcTemplate.update("""
                insert into balances(subscriber_id, amount, version)
                values(?, 1000.0000, 0)
                """, subscriberId);
    }

    private Cdr createCdr(){
        return new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now().minusSeconds(60), Instant.now());
    }

    @Test
    void successfulChargeWritesUnpublishedOutboxRow() throws Exception {
        Cdr cdr = createCdr();

        ChargeResult result = chargingService.process(cdr);

        assertThat(result.status()).isEqualTo(ChargeStatus.CHARGED);

        List<OutboxEventEntity> rows = outboxEventRepository.findAll();
        assertThat(rows.size()).isEqualTo(1);
        OutboxEventEntity row = rows.getFirst();
        assertThat(row.isPublished()).isFalse();
        assertThat(row.getAggregateId()).isEqualTo("9876543000");
        assertThat(row.getEventType()).isEqualTo(OutboxWriter.EVENT_TYPE_CDR_CHARGED);

        CdrEvent cdrEvent = objectMapper.readValue(row.getPayload(), CdrEvent.class);
        assertThat(cdrEvent.eventId()).isEqualTo(cdr.eventId());
        assertThat(cdrEvent.msisdn()).isEqualTo(cdr.msisdn());
        assertThat(cdrEvent.eventTimestamp()).isEqualTo(cdr.eventTimestamp());
        assertThat(cdrEvent.receivedAt()).isEqualTo(cdr.receivedAt());
    }


    @Test
    void pollerPublishThanMarkPublished(){
        when(kafkaTemplate.send(any(), any(), any(CdrEvent.class)))
                .thenAnswer(invocation -> {
                    String topic = invocation.getArgument(0);
                    String key = invocation.getArgument(1);
                    CdrEvent event = invocation.getArgument(2);
                    RecordMetadata metaData = new RecordMetadata(
                            new TopicPartition(topic, 0), 0L, 0,
                            System.currentTimeMillis(),0,0);
                    return CompletableFuture.completedFuture(
                            new SendResult<>(new ProducerRecord<>(topic,key,event), metaData));
                });

        ChargeResult result = chargingService.process(createCdr());
        assertThat(result.status()).isEqualTo(ChargeStatus.CHARGED);

        assertThat(outboxEventRepository.countByPublishedFalse()).isEqualTo(1);

        outboxPoller.publishBatch();

        assertThat(outboxEventRepository.countByPublishedFalse()).isEqualTo(0);

        verify(kafkaTemplate, times(1))
                .send(eq("cdr-events"),eq("9876543000"), any(CdrEvent.class));
    }

    @Test
    void nonChargedDoesNotWriteOutBox(){
        Cdr unknown = new Cdr(UUID.randomUUID(), "acme", "0000000000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        ChargeResult result = chargingService.process(unknown);
        assertThat(result.status()).isEqualTo(ChargeStatus.SUBSCRIBER_NOT_FOUND);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }
}
