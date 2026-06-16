package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncChargingServiceTest {

    /*private AsyncChargingService service;
    private SubscriberBalance balance;
    private ExecutorService executor;

    @BeforeEach
    void setUp(){
        executor = Executors.newFixedThreadPool(4);
        balance = new SubscriberBalance("9876543210", new BigDecimal("100"));

        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        service = new AsyncChargingService( new RatingEngine(),
                Map.of("9876543210", balance),
                Map.of("acme", plan),
                executor);
    }

    private Cdr makeCdr(UsageType type, String quantity){
        return makeCdr(UUID.randomUUID(), type, quantity);
    }

    private Cdr makeCdr(UUID eventId, UsageType type, String quantity) {
        return new Cdr(eventId, "acme", "9876543210",
                type, new BigDecimal(quantity), Instant.now(), Instant.now());
    }

    @Test
    void shouldChargeAsynchronously(){
        Cdr cdr = makeCdr(UsageType.VOICE, "120");

        ChargeResult result = service.processAsync(cdr).join();
        assertEquals(ChargeStatus.CHARGED, result.status());
        assertEquals(0, new BigDecimal("1.2000").compareTo(result.amountCharged()));
    }

    @Test
    void shouldDetectDuplicateAsync(){
        UUID eventID = UUID.randomUUID();
        Cdr cdr1 = makeCdr(eventID, UsageType.VOICE, "60");
        Cdr cdr2 = makeCdr(eventID, UsageType.VOICE, "60");

        ChargeResult first = service.processAsync(cdr1).join();
        ChargeResult second = service.processAsync(cdr2).join();

        assertEquals(ChargeStatus.CHARGED, first.status());
        assertEquals(ChargeStatus.DUPLICATE, second.status());
    }

    @Test
    void shouldHandleSubscriberNotFound(){
        Cdr cdr = new Cdr( UUID.randomUUID(), "acme",
                "9876543211", UsageType.SMS, new BigDecimal("1"),
                Instant.now(),Instant.now());
        ChargeResult result = service.processAsync(cdr).join();

        assertEquals(ChargeStatus.SUBSCRIBER_NOT_FOUND , result.status());
    }

    @Test
    void shouldHandleInsufficientBalance(){
        Cdr cdr = makeCdr(UsageType.DATA, "500");
        ChargeResult result = service.processAsync(cdr).join();

        assertEquals( ChargeStatus.INSUFFICIENT_BALANCE, result.status());
    }*/
}
