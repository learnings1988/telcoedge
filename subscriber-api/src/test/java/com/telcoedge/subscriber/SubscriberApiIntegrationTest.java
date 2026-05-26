package com.telcoedge.subscriber;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.domain.SubscriberStatus;
import com.telcoedge.subscriber.exception.SubscriberAlreadyExistException;
import com.telcoedge.subscriber.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SubscriberApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    SubscriberService service;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createSubscriberAndReadItBack() {
        Subscriber created = service.create("acme", "919876543210", "Aakash kumar");
        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(SubscriberStatus.ACTIVE);

        Subscriber found = service.findByMsisdn("acme", "919876543210");
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Aakash kumar");

    }

    @Test
    void returns409WhenDuplicateSubscriber(){
        service.create("testtop", "919111000001",
                "First");

        assertThatThrownBy(()-> service.create("testtop", "919111000001",
                "Second")).isInstanceOf(SubscriberAlreadyExistException.class);
    }

    @Test
    void returns404ForUnknownSubscriber(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/operators/acme/subscribers/00000000000",
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(
                404);
    }
}
