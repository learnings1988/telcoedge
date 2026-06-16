package com.telcoedge.charging.persistence;

import com.telcoedge.charging.persistence.BalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceRepository extends JpaRepository<BalanceEntity, Long> {
    Optional<BalanceEntity> findBySubscriberId(Long subscriberId);
}
