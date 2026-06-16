package com.telcoedge.charging.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {


    Optional<IdempotencyKeyEntity> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query("DELETE FROM IdempotencyKeyEntity k where k.expiresAt < :cutOff")
    int deleteExpiredKeys(@Param("cutOff") Instant cutOff);
}
