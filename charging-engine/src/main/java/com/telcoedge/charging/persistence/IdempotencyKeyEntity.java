package com.telcoedge.charging.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "response_hash", length = 64)
    private String responseHash;

    @Column(name = "created_at", nullable = false, updatable = false )
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false )
    private Instant expiresAt;

    protected IdempotencyKeyEntity(){}

    public IdempotencyKeyEntity(UUID eventId, String status, Instant createdAt, Instant expiresAt) {
        this.eventId = eventId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public IdempotencyKeyEntity(UUID eventId, String status) {
        this.eventId = eventId;
        this.status = status;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(86400);

    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public String getResponseHash() {
        return responseHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
