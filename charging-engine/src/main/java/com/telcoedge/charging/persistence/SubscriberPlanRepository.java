package com.telcoedge.charging.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriberPlanRepository extends JpaRepository<SubscriberPlanEntity, Long> {

    @Query("""
    SELECT new com.telcoedge.charging.persistence.SubscriberToPlanIdDto(
    spe.planId
    )FROM SubscriberPlanEntity spe
    WHERE spe.subscriberId = :subscriberId AND spe.active=true
    """)
    Optional<SubscriberToPlanIdDto> findBySubscriberId(@Param("subscriberId") Long subscriberId);
}
