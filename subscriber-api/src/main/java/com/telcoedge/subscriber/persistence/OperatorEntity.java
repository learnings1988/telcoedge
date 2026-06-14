package com.telcoedge.subscriber.persistence;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="operators")
public class OperatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OperatorEntity() {}

    public OperatorEntity(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public void deactivate(){this.active = false;}
}
