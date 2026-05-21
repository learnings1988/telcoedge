package com.telcoedge.subscriber.persistence;

import com.telcoedge.domain.Subscriber;

public final class SubscriberMapper {
    private SubscriberMapper(){}

    public static Subscriber toDomain( SubscriberEntity e){
        return new Subscriber(e.getId(),e.getOperatorId(),e.getMsisdn(),e.getName(),
        e.getStatus(),e.getCreatedAt(),e.getUpdatedAt());
    }

    public static SubscriberEntity toEntity( Subscriber s){
        return new SubscriberEntity( s.operatorId(), s.msisdn(), s.name(), s.status());
    }
}
