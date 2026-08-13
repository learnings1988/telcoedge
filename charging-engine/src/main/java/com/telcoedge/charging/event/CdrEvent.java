package com.telcoedge.charging.event;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.UsageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CdrEvent(UUID eventId,
                       String operatorId,
                       String msisdn,
                       UsageType usageType,
                       BigDecimal quantity,
                       Instant eventTimestamp,
                       Instant receivedAt) {

    public static CdrEvent fromCdr(Cdr cdr){
        return new CdrEvent(cdr.eventId(), cdr.operatorId(), cdr.msisdn(), cdr.usageType(),
                cdr.quantity(), cdr.eventTimestamp(), cdr.receivedAt());
    }

    public Cdr toCdr(){
        return new Cdr(eventId, operatorId, msisdn, usageType,quantity, eventTimestamp, receivedAt);
    }
}
