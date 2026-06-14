package com.telcoedge.subscriber.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceRepository extends JpaRepository<BalanceEntity, Long> {
    Optional<BalanceEntity> findBySubscriberId(Long subscriberId);
}
