package com.telcoedge.domain;

import java.time.Instant;

public record Subscriber(
        Long id ,
        String operatorId,
        String msisdn,
        String name,
        SubscriberStatus status,
        Instant createdAt,
        Instant updatedAt
){
    public Subscriber{
        if( operatorId == null || operatorId.isBlank()){
            throw new IllegalArgumentException("OperatorId required");
        }
        if( msisdn == null || msisdn.isBlank()){
            throw new IllegalArgumentException("MSISDN Required");
        }
        if( status==null){
            throw new IllegalArgumentException("status required");
        }
    }
}
