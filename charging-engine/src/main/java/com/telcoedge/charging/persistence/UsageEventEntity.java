package com.telcoedge.charging.persistence;

import com.telcoedge.domain.UsageType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_events")
public class UsageEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "operator_id", nullable = false, length = 64)
    private String operatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 10)
    private UsageType usageType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal units;

    @Column(name = "rate_applied", nullable = false, precision = 10, scale = 4)
    private BigDecimal rateApplied;

    @Column(name = "amount_charged", nullable = false, precision = 15, scale = 4)
    private BigDecimal amountCharged;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected UsageEventEntity(){}

    public UsageEventEntity( UUID eventId, Long subscriberId, String operatorId,
                            UsageType usageType, BigDecimal units, BigDecimal rateApplied,
                            BigDecimal amountCharged, BigDecimal balanceAfter,
                            String status, Instant processedAt) {
        this.eventId = eventId;
        this.subscriberId = subscriberId;
        this.operatorId = operatorId;
        this.usageType = usageType;
        this.units = units;
        this.rateApplied = rateApplied;
        this.amountCharged = amountCharged;
        this.balanceAfter = balanceAfter;
        this.status = status;
        this.processedAt = processedAt;
    }


    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public BigDecimal getRateApplied() {
        return rateApplied;
    }

    public BigDecimal getAmountCharged() {
        return amountCharged;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
