package com.telcoedge.charging.persistence;

import com.telcoedge.domain.UsageType;

import java.math.BigDecimal;

public interface TariffRateView {
    UsageType getUsageType();
    BigDecimal getRatePerUnit();
}
