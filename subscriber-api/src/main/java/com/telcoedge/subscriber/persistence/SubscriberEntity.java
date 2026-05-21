package com.telcoedge.subscriber.persistence;


import com.telcoedge.domain.SubscriberStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "subscribers")
public class SubscriberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false, length = 64)
    private String operatorId;

    @Column(name = "msisdn", nullable = false, length = 20)
    private String msisdn;

    @Column( nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column( nullable = false, length = 20)
    private SubscriberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubscriberEntity(){}

    public SubscriberEntity( String operatorId, String msisdn, String name,
                            SubscriberStatus status) {
        this.operatorId = operatorId;
        this.msisdn = msisdn;
        this.name = name;
        this.status = status;
    }

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = Instant.now();
    }


    public Long getId() {
        return id;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getName() {
        return name;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
