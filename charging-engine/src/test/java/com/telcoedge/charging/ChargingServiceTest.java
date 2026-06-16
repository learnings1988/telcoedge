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

import static org.junit.jupiter.api.Assertions.*;

public class ChargingServiceTest {

   /* private ChargingService chargingService;
    private SubscriberBalance subscriberBalance;

    @BeforeEach
    void setUp(){
        subscriberBalance = new SubscriberBalance("9876543210", new BigDecimal("100"));
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        chargingService = new ChargingService(new RatingEngine(),
                Map.of("9876543210", subscriberBalance),
                Map.of("acme", plan)
        );
    }

    private Cdr makeCdr(UsageType type, String quantity){
        return makeCdr(UUID.randomUUID(), type, quantity);
    }

    private Cdr makeCdr(UUID eventId, UsageType type, String quantity) {
        return new Cdr(eventId, "acme", "9876543210",
                type, new BigDecimal(quantity), Instant.now(), Instant.now());
    }

    @Test
    void shouldChargeSuccessfully(){
        Cdr cdr = makeCdr(UsageType.VOICE, "120");
        ChargeResult result = chargingService.process( cdr);

        assertEquals(ChargeStatus.CHARGED , result.status());
        assertEquals(0, new BigDecimal("1.2000" ).compareTo(result.amountCharged()));
        assertEquals(0, new BigDecimal("98.8000").compareTo(result.remainingBalance()));
    }

    @Test
    void shouldRejectInsufficientBalance(){
        Cdr cdr = makeCdr(UsageType.DATA, "500");

        ChargeResult result = chargingService.process(cdr);

        assertEquals(ChargeStatus.INSUFFICIENT_BALANCE , result.status());
        assertEquals(0,
                new BigDecimal("100").compareTo(subscriberBalance.getBalance()));
    }

    @Test
    void shouldReturnDuplicateForSameEventId(){
        UUID eventId = UUID.randomUUID();
        Cdr cdr1 = makeCdr(eventId, UsageType.VOICE, "60");
        Cdr cdr2 = makeCdr(eventId, UsageType.VOICE, "60");

        ChargeResult firstResult = chargingService.process(cdr1);
        ChargeResult secondResult = chargingService.process(cdr2);

        assertEquals(ChargeStatus.CHARGED , firstResult.status());
        assertEquals(ChargeStatus.DUPLICATE , secondResult.status());

        assertEquals(0,
                new BigDecimal("99.4000").compareTo(subscriberBalance.getBalance()));
    }

    @Test
    void shouldReturnSubscriberNotFound(){
        Cdr cdr = new Cdr(UUID.randomUUID(),"acme", "9876543211",
                UsageType.SMS, new BigDecimal("1"), Instant.now(), Instant.now());

        ChargeResult result = chargingService.process(cdr);
        assertEquals(ChargeStatus.SUBSCRIBER_NOT_FOUND, result.status());
    }*/
}
