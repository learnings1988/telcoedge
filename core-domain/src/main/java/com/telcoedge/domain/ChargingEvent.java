package com.telcoedge.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface ChargingEvent permits ChargingEvent.Charged,
        ChargingEvent.Failed, ChargingEvent.Duplicate{

    UUID eventId();
    String msisdn();
    Instant occurredAt();

    record Charged(UUID eventId, String msisdn, BigDecimal amount, BigDecimal remainingBalance,
                   Instant occurredAt) implements ChargingEvent {}

    record Failed(UUID eventId, String msisdn, BigDecimal attemptedAmount,
                  FailureReason reason, Instant occurredAt) implements ChargingEvent {}

    record Duplicate(UUID eventId, String msisdn, UUID originalEventId,
                     Instant occurredAt) implements ChargingEvent {}

    enum FailureReason{
        INSUFFICIENT_BALANCE,
        SUBSCRIBER_NOT_FOUND,
        TARIFF_NOT_FOUND
    }

}
