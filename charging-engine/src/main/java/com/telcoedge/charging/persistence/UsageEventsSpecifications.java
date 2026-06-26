package com.telcoedge.charging.persistence;

import com.telcoedge.domain.UsageType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class UsageEventsSpecifications {

    private UsageEventsSpecifications(){}

    public static Specification<UsageEventEntity> hasSubscriber( Long subscriberId){
        return (root, query, cb) ->
                cb.equal(root.get("subscriberId"), subscriberId);
    }

    public static Specification<UsageEventEntity> hasUsageType(UsageType usageType){
        return (root, query, cb) ->
                cb.equal(root.get("usageType"), usageType);
    }

    public static Specification<UsageEventEntity> hasStatus(String status){
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<UsageEventEntity> processedAfter(Instant from){
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("processedAt"), from);
    }

    public static Specification<UsageEventEntity> processedBefore(Instant to){
        return (root, query, cb) ->
                cb.lessThan(root.get("processedAt"), to);
    }
}
