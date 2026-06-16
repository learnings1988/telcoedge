package com.telcoedge.charging.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "subscriber_plans")
public class SubscriberPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private boolean active;

    protected SubscriberPlanEntity(){}

    public Long getId() {
        return id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public Long getPlanId() {
        return planId;
    }

    public boolean isActive() {
        return active;
    }
}
