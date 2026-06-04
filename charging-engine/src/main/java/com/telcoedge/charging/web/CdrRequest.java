package com.telcoedge.charging.web;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CdrRequest(UUID eventId,
                         String operatorId,
                         String msisdn,
                         UsageType usageType,
                         BigDecimal quantity,
                         Instant startTime,
                         Instant endTime) {

    public Cdr toCdr(){
        return new Cdr(eventId, operatorId, msisdn, usageType, quantity,
                startTime, endTime);
    }
}
