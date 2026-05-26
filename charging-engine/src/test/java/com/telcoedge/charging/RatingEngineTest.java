package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingEngineTest {
    private final TariffPlan plan = new TariffPlan("basic",
            "basic plan",
            new BigDecimal("0.01"),
            new BigDecimal("0.50"),
            new BigDecimal("1.00")
    );

    private final RatingEngine ratingEngine = new RatingEngine();

    @Test
    void shouldChargeVoiceByDuration(){
        Cdr cdr = new Cdr( UUID.randomUUID(), "acme", "9876543210",
                UsageType.VOICE, new BigDecimal("120"), Instant.now(), Instant.now());

        BigDecimal charge = ratingEngine.calculateCharge( cdr, plan);
        assertEquals(new BigDecimal("1.2000"), charge);
    }

    @Test
    void shouldChargeDataByMegaBytes(){
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543210",
                UsageType.DATA, new BigDecimal("50"), Instant.now(), Instant.now());

        BigDecimal charge = ratingEngine.calculateCharge( cdr, plan);
        assertEquals(new BigDecimal("25.0000"), charge);
    }

    @Test
    void shouldChargeSmsPerMessage(){
        Cdr cdr = new Cdr(UUID.randomUUID(), "acme", "9876543210",
                UsageType.SMS, new BigDecimal("3"), Instant.now(), Instant.now());

        BigDecimal charge = ratingEngine.calculateCharge( cdr, plan);
        assertEquals(new BigDecimal("3.0000"), charge);
    }
}