package com.telcoedge.subscriber.persistence;


import com.telcoedge.domain.UsageType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tariff_rates")
public class TariffRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Enumerated(EnumType.STRING)
    @Column(name="usage_type", nullable = false, length = 10)
    private UsageType usageType;

    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(name = "created_at", nullable = false, updatable = false )
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false )
    private Instant updatedAt;

    protected TariffRateEntity(){}

    public TariffRateEntity(UsageType usageType, BigDecimal ratePerUnit) {
        this.usageType = usageType;
        this.ratePerUnit = ratePerUnit;
    }

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        createdAt = now;
        updatedAt =now;
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = Instant.now();
    }

    void setPlan(PlanEntity plan){this.plan = plan;}

    public Long getId() {
        return id;
    }

    public PlanEntity getPlan() {
        return plan;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public BigDecimal getRatePerUnit() {
        return ratePerUnit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
