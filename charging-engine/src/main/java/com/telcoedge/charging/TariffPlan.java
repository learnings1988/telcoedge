package com.telcoedge.charging;

import java.math.BigDecimal;

public record TariffPlan(
        String planId,
        String planName,
        BigDecimal voiceRatePerSecond,
        BigDecimal dataRatePerMb,
        BigDecimal smsRatePerMessage
) {
}
