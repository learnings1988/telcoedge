package com.telcoedge.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ChargeResult(
        UUID eventId,
        String msisdn,
        BigDecimal amountCharged,
        BigDecimal remainingBalance,
        ChargeStatus status,
        Instant processedAt
) {
}
