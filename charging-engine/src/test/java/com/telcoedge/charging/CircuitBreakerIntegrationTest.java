package com.telcoedge.charging;


import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CircuitBreakerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SubscriberLookup subscriberLookup;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void circuitOpensAfterRepeatedFailures(){
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("subscriberLookup");

        for(int i=0;i<10;i++){
            subscriberLookup.findSubscriberId("nonexistent", "0000000000");
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void fallbackReturnsNullWhenCircuitOpen(){
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("subscriberLookup");
        cb.transitionToOpenState();
        Long result = subscriberLookup.findSubscriberId("acme", "9876543210");
        assertThat(result).isNull();
    }
}
