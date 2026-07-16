package com.telcoedge.subscriber;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.domain.SubscriberStatus;
import com.telcoedge.subscriber.config.TestSecurityConfig;
import com.telcoedge.subscriber.exception.SubscriberAlreadyExistException;
import com.telcoedge.subscriber.persistence.OperatorEntity;
import com.telcoedge.subscriber.persistence.OperatorRepository;
import com.telcoedge.subscriber.service.SubscriberService;
import com.telcoedge.subscriber.testutil.JWTTestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
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
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    SubscriberService service;


    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void seedOperators(@Autowired OperatorRepository operatorRepository){
        operatorRepository.save(new OperatorEntity("testtop", "Test Operator"));
    }

    private HttpHeaders authHeaders(String operatorId){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JWTTestUtil.operatorToken(operatorId));
        return headers;
    }

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
        ResponseEntity<String> response = restTemplate
                .exchange("/api/v1/operators/acme/subscribers/00000000000"
                        , HttpMethod.GET
                , new HttpEntity<>(authHeaders("acme"))
                ,String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(
                404);
    }

    @Test
    void rejectRequestWithoutToken(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/operators/acme/subscribers/00000000000", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void healthEndPointIsPublic(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
