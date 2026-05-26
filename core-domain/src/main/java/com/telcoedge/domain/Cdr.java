package com.telcoedge.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Cdr(
        UUID eventId,
        String operatorId,
        String msisdn,
        UsageType usageType,
        BigDecimal quantity,
        Instant eventTimestamp,
        Instant receivedAt
) {
    public Cdr{
        if(eventId == null) throw new IllegalArgumentException("eventid is required");
        if(msisdn == null || msisdn.isBlank()) throw new IllegalArgumentException("msisdn is required");
        if(quantity == null || quantity.compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("quantity must be positive number");
    }
}
