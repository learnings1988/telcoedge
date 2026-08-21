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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@SpringBootTest
@Testcontainers
public class OutboxIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers" , kafka::getBootstrapServers);
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

    @Value("${spring.kafka.bootstrap-servers}")
    String resolvedBootstrap;

    private Consumer<String, CdrEvent> testConsumer;

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

    @AfterEach
    void closeConsumer(){
        if(testConsumer != null){
            testConsumer.close();
            testConsumer = null;
        }
    }

    private Cdr createCdr(){
        return new Cdr(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now().minusSeconds(60), Instant.now());
    }

    private Consumer<String, CdrEvent> newTestConsumer(){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-it");
        props.put(ConsumerConfig
                .AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        JsonDeserializer<CdrEvent> valueDeserializer =
                new JsonDeserializer<>(CdrEvent.class, false);

        valueDeserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer).createConsumer();
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
        testConsumer = newTestConsumer();
        testConsumer.subscribe(List.of("cdr-events"));

        Cdr cdr = createCdr();

        ChargeResult result = chargingService.process(cdr);
        assertThat(result.status()).isEqualTo(ChargeStatus.CHARGED);

        assertThat(outboxEventRepository.countByPublishedFalse()).isEqualTo(1);

        outboxPoller.publishBatch();

        ConsumerRecord<String, CdrEvent> recordFromKafka =
                KafkaTestUtils.getSingleRecord(testConsumer, "cdr-events", Duration.ofSeconds(10));

        assertThat(recordFromKafka.key()).isEqualTo(cdr.msisdn());
        assertThat(recordFromKafka.value().eventId()).isEqualTo(cdr.eventId());
        assertThat(outboxEventRepository.countByPublishedFalse()).isEqualTo(0);

    }

    @Test
    void nonChargedDoesNotWriteOutBox(){
        Cdr unknown = new Cdr(UUID.randomUUID(), "acme", "0000000000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        ChargeResult result = chargingService.process(unknown);
        assertThat(result.status()).isEqualTo(ChargeStatus.SUBSCRIBER_NOT_FOUND);
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void bootstrapPointsToContainer(){
        System.out.println(STR."Resolved bootstrap = \{resolvedBootstrap}");
        System.out.println(STR."contianer bootstrap = \{kafka.getBootstrapServers()}");
        assertThat(resolvedBootstrap).isEqualTo(kafka.getBootstrapServers());
    }
}