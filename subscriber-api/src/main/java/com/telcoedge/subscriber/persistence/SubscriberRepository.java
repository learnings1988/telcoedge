package com.telcoedge.subscriber.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriberRepository  extends JpaRepository<SubscriberEntity, Long> {

    Optional<SubscriberEntity> findByOperatorIdAndMsisdn(String operatorId, String msisdn);
    boolean existsByOperatorIdAndMsisdn(String operatorId, String msisdn);
}
