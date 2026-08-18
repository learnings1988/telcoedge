package com.telcoedge.charging.event;


import com.telcoedge.domain.UsageType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CdrEventConsumerTest.TestApp.class)
@EmbeddedKafka(partitions = 1, topics = { "cdr-events" })
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=charging-engine-downstream-test",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.telcoedge.charging.event",
        "spring.kafka.consumer.properties.spring.json.value.default.type=com.telcoedge.charging.event.CdrEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=true",
        "management.endpoint.health.validate-group-membership=false"
})
public class CdrEventConsumerTest {

    @Autowired
    KafkaTemplate<String, CdrEvent> kafkaTemplate;

    @Autowired
    CdrEventConsumer consumer;

    @Autowired
    MeterRegistry meterRegistry;


    private double duplicateCount(){
        var counter = meterRegistry.find("cdr.consumed").tag("outcome", "duplicate").counter();
        return counter== null ? 0.0 : counter.count();
    }

    @Test
    void consumeOnceAndDedupesDuplicate(){
        UUID eventId = UUID.randomUUID();

        CdrEvent event = new CdrEvent(eventId, "acme", "9876543000",
                UsageType.VOICE, new BigDecimal("60"), Instant.now(), Instant.now());

        kafkaTemplate.send("cdr-events", event.msisdn(), event);
        kafkaTemplate.send("cdr-events", event.msisdn(), event);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(()->{
            assertThat(consumer.hasProcessed(eventId)).isTrue();
            assertThat(duplicateCount()).isEqualTo(1.0);
        });

        assertThat(consumer.processedCount()).isEqualTo(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude ={
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import(CdrEventConsumer.class)
    static class TestApp{
        @Bean
        MeterRegistry meterRegistry(){
            return new SimpleMeterRegistry();
        }
    }
}
