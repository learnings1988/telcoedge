package com.telcoedge.charging.persistence;


import com.telcoedge.domain.UsageType;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_rates")
public class TariffRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 10)
    private UsageType usageType;

    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerUnit;

    protected TariffRateEntity(){}

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public BigDecimal getRatePerUnit() {
        return ratePerUnit;
    }
}
