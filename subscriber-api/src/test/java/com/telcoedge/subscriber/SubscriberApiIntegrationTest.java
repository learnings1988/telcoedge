package com.telcoedge.subscriber;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.domain.SubscriberStatus;
import com.telcoedge.subscriber.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SubscriberApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    SubscriberService service;

    @Test
    void createSubscriberAndReadItBack() {
        Subscriber created = service.create("acme", "919876543210", "Aakash kumar");
        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(SubscriberStatus.ACTIVE);

        Subscriber found = service.findByMsisdn("acme", "919876543210");
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Aakash kumar");
    }
}
