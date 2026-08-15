package com.telcoedge.charging.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    long countByPublishedFalse();

    @Query(value = """
    select * from outbox_events
    where published=false
    order by create_at asc
    limit :limit
    for update skip locked
    """, nativeQuery = true)
    List<OutboxEventEntity> findUnpublishedForUpdate(@Param("limit") int limit);
}
