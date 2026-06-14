package com.telcoedge.subscriber.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEventEntity, Long> {

    Optional<UsageEventEntity> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);
}
