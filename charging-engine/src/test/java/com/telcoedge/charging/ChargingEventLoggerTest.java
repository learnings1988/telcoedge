package com.telcoedge.charging;


import com.telcoedge.domain.ChargingEvent;
import com.telcoedge.domain.ChargingEvent.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChargingEventLoggerTest {

    private final ChargingEventLogger logger = new ChargingEventLogger();

    @Test
    void shouldDescribeNormalCharge(){
        var event = new Charged( UUID.randomUUID(), "9876543210", new BigDecimal("1.20"),
                new BigDecimal("98.80"), Instant.now());

        String desc = logger.describeEvent(event);
        assertTrue(desc.startsWith("charged: 1.20"));
    }

    @Test
    void shouldHighValueCharge(){
        var event = new Charged( UUID.randomUUID(), "9876543210", new BigDecimal("750.00"),
                new BigDecimal("250.00"), Instant.now());

        String desc = logger.describeEvent(event);
        assertTrue(desc.startsWith("High value"));
    }

    @Test
    void shouldDescribeFailure(){
        var event = new Failed( UUID.randomUUID(), "9876543210", new BigDecimal("500.00"),
                FailureReason.INSUFFICIENT_BALANCE, Instant.now());

        String desc = logger.describeEvent(event);
        assertTrue(desc.startsWith("Failed"));
    }

    @Test
    void shouldDescribeDuplicate(){
        UUID origEventId = UUID.randomUUID();
        var event = new Duplicate( UUID.randomUUID(), "9876543210", origEventId,
                Instant.now());

        String desc = logger.describeEvent(event);
        assertTrue(desc.contains(origEventId.toString()));
    }

}
