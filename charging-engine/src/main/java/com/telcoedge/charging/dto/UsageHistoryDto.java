package com.telcoedge.charging.dto;

import com.telcoedge.domain.UsageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UsageHistoryDto(
        UUID eventId,
        UsageType usageType,
        BigDecimal units,
        BigDecimal rateApplied,
        BigDecimal amountCharged,
        BigDecimal balanceAfter,
        Instant processedAt
) {
}
