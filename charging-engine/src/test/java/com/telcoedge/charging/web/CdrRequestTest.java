package com.telcoedge.charging.web;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

public class CdrRequestTest {

    @Test
    void toCdrMapsStartTimeTOEventTimestamp(){
        Instant callStart = Instant.now().minus(Duration.ofHours(1));
        Instant callEnd = callStart.plusSeconds(120);

        CdrRequest request = new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal(120), callStart, callEnd);

        Cdr cdr = request.toCdr();

        assertThat(cdr.eventTimestamp()).isEqualTo(callStart);
    }

    @Test
    void toCdrSetReceivedAtToTimeOfIngestNotCallEndTime(){
        Instant callStart = Instant.now().minus(Duration.ofHours(1));
        Instant callEnd = callStart.plusSeconds(120);

        CdrRequest request = new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal(120), callStart, callEnd);

        Instant before = Instant.now();
        Cdr cdr = request.toCdr();
        Instant after = Instant.now();

        assertThat(cdr.receivedAt()).isNotEqualTo(callEnd);
        assertThat(cdr.receivedAt()).isBetween(before, after);
    }

    @Test
    void mediationLagIsIngestTimeNotCallDuration(){
        Instant callStart = Instant.now().minus(Duration.ofHours(1));
        Instant callEnd = callStart.plusSeconds(120);

        CdrRequest request = new CdrRequest(UUID.randomUUID(), "acme", "9876543000",
                UsageType.VOICE, new BigDecimal(120), callStart, callEnd);

        Cdr cdr = request.toCdr();

        Long lagSeconds = cdr.receivedAt().getEpochSecond() - cdr.eventTimestamp().getEpochSecond();
        assertThat(lagSeconds).isGreaterThan(3599L);
    }

    @Test
    void everyOtherFieldPassThroughUnchanged(){
        UUID eventId = UUID.randomUUID();
        CdrRequest request = new CdrRequest(eventId, "acme", "9876543000",
                UsageType.DATA, new BigDecimal(512.50), Instant.now().minusSeconds(120), Instant.now());

        Cdr cdr = request.toCdr();

        assertThat(cdr.eventId()).isEqualTo(eventId);
        assertThat(cdr.operatorId()).isEqualTo("acme");
        assertThat(cdr.msisdn()).isEqualTo("9876543000");
        assertThat(cdr.usageType()).isEqualTo(UsageType.DATA);
        assertThat(cdr.quantity()).isEqualByComparingTo(new BigDecimal("512.50"));

    }
}
