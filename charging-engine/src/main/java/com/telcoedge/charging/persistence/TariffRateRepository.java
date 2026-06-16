package com.telcoedge.charging.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TariffRateRepository extends JpaRepository<TariffRateEntity, Long> {

    @Query("""
    SELECT tr.usageType AS usageType, tr.ratePerUnit AS ratePerUnit
    FROM TariffRateEntity tr
    WHERE tr.planId = (
    SELECT sp.planId FROM SubscriberPlanEntity sp
    WHERE sp.subscriberId = :subscriberId AND sp.active=true
    )
""")
    List<TariffRateView> findActiveRatesForSubscriber(@Param("subscriberId") Long subscriberId);
}
