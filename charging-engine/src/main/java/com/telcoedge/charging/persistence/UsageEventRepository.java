package com.telcoedge.charging.persistence;

import com.telcoedge.charging.dto.UsageHistoryDto;
import com.telcoedge.charging.persistence.UsageEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEventEntity, Long> {

    Optional<UsageEventEntity> findByEventId(UUID eventId);
    boolean existsByEventId(UUID eventId);

    @Query("""
    SELECT new com.telcoedge.charging.dto.UsageHistoryDto(
        ue.eventId, ue.usageType, ue.units, ue.rateApplied,
        ue.amountCharged, ue.balanceAfter, ue.processedAt)
    FROM UsageEventEntity ue
    WHERE ue.subscriberId = :subscriberId
    ORDER BY ue.processedAt DESC
""")
    Page<UsageHistoryDto> findHistoryBySubscriber(
            @Param("subscriberId") Long subscriberId, Pageable pageable);

    @Query("""
    SELECT new com.telcoedge.charging.dto.UsageHistoryDto(
        ue.eventId, ue.usageType, ue.units, ue.rateApplied,
        ue.amountCharged, ue.balanceAfter, ue.processedAt)
    FROM UsageEventEntity ue
    WHERE ue.subscriberId = :subscriberId AND ue.usageType=:usageType
    ORDER BY ue.processedAt DESC
""")
    Page<UsageHistoryDto> findHistoryBySubscriberIdAndType(
            @Param("subscriberId") Long subscriberId,
            @Param("usageType") String usageType,
            Pageable pageable
    );

}
