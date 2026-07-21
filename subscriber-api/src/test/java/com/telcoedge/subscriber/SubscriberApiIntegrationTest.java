package com.telcoedge.subscriber;

import com.telcoedge.domain.Subscriber;
import com.telcoedge.domain.SubscriberStatus;
import com.telcoedge.subscriber.exception.SubscriberAlreadyExistException;
import com.telcoedge.subscriber.persistence.OperatorEntity;
import com.telcoedge.subscriber.persistence.OperatorRepository;
import com.telcoedge.subscriber.service.SubscriberService;
import com.telcoedge.subscriber.testutil.JWTTestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void seedOperators(@Autowired OperatorRepository operatorRepository){
        operatorRepository.save(new OperatorEntity("testtop", "Test Operator"));
    }

    private HttpHeaders authHeaders(String operatorId){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JWTTestUtil.operatorToken(operatorId));
        return headers;
    }

    private HttpHeaders jsonHeaders(HttpHeaders existing){
        HttpHeaders headers = new HttpHeaders(existing);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void createSubscriberAndReadItBack() {

        HttpHeaders acmeHeaders = authHeaders("acme");
        String body = """
                {"msisdn": "9876543210", "name": "Aakash kumar"}
                """;

        ResponseEntity<Subscriber> createResponse = restTemplate.exchange(
                "/api/v1/operators/acme/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(acmeHeaders)),
                Subscriber.class
        );

        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().id()).isNotNull();
        assertThat(createResponse.getBody().status()).isEqualTo(SubscriberStatus.ACTIVE);


        ResponseEntity<Subscriber> readResponse = restTemplate.exchange(
                "/api/v1/operators/acme/subscribers/9876543210",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders("acme")),
                Subscriber.class
        );

        assertThat(readResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(readResponse.getBody()).isNotNull();
        assertThat(readResponse.getBody().id()).isEqualTo(
                createResponse.getBody().id());
        assertThat(readResponse.getBody().name()).isEqualTo(
                createResponse.getBody().name());
    }

    @Test
    void returns409WhenDuplicateSubscriber(){

        HttpHeaders testtopHeaders = authHeaders("testtop");
        String body = """
                {"msisdn": "919111000001", "name": "first"}
                """;

        ResponseEntity<Subscriber> createResponse = restTemplate.exchange(
                "/api/v1/operators/testtop/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(testtopHeaders)),
                Subscriber.class
        );


        ResponseEntity<String> duplicateCreateResponse = restTemplate.exchange(
                "/api/v1/operators/testtop/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(testtopHeaders)),
                String.class
        );
        assertThat(duplicateCreateResponse.getStatusCode().value()).isEqualTo(409);
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

    @Test
    void allowSameOperatorTest(){
        HttpHeaders acmeHeaders = authHeaders("acme");
        String body = """
                {"msisdn": "9876543219", "name": "Test User"}
                """;

        ResponseEntity<String> createResponse = restTemplate.exchange(
          "/api/v1/operators/acme/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(acmeHeaders)),
                String.class
        );

        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);


        ResponseEntity<String> readResponse = restTemplate.exchange(
                "/api/v1/operators/acme/subscribers/9876543219",
                HttpMethod.GET,
                new HttpEntity<>(acmeHeaders),
                String.class
        );

        assertThat(readResponse.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void deniedCrossTenantAccess(){
        HttpHeaders acmeHeaders = authHeaders("acme");
        String body = """
                {"msisdn": "9876543999", "name": "Test User"}
                """;
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/v1/operators/acme/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(acmeHeaders)),
                String.class
        );

        HttpHeaders zenithHeader = authHeaders("zenith");
        ResponseEntity<String> crossTenantResponse = restTemplate.exchange(
          "/api/v1/operators/acme/subscribers/9876543999",
          HttpMethod.GET,
          new HttpEntity<>(zenithHeader),
          String.class
        );
        assertThat(crossTenantResponse.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void deniedCrossTenantCreate(){
        HttpHeaders zenithHeaders = authHeaders("zenith");
        String body = """
                {"msisdn": "9876540000", "name": "sneaky User"}
                """;

        ResponseEntity<String> crossTemplateCreateResponse = restTemplate.exchange(
          "/api/v1/operators/acme/subscribers",
          HttpMethod.POST,
          new HttpEntity<>(body, jsonHeaders(zenithHeaders)),
          String.class
        );
        assertThat(crossTemplateCreateResponse.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    public void createdByIsCapturedFromJwt(){
        HttpHeaders acmeHeaders = authHeaders("acme");
        String body = """
                {"msisdn": "9876500001", "name": "Audit Test User"}
                """;
        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/v1/operators/acme/subscribers",
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(acmeHeaders)),
                String.class
        );

        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);

        String createdBy = jdbcTemplate.queryForObject(
                "SELECT created_by FROM subscribers where msisdn = '9876500001'",
                String.class);

        assertThat(createdBy).isEqualTo("api-user");
    }
}
