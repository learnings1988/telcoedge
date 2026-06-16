package com.telcoedge.charging.persistence;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balances")
public class BalanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false, unique = true)
    private Long subscriberId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Version
    private int version;

    @Column(name = "created_at" , nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BalanceEntity (){}

    public BalanceEntity(Long subscriberId, BigDecimal amount) {
        this.subscriberId = subscriberId;
        this.amount = amount;
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

    public boolean deduct(BigDecimal charge){
        if(amount.compareTo(charge)<0){
            return false;
        }
        amount = amount.subtract(charge);
        return true;
    }

    public Long getId() {
        return id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
