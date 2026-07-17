package com.telcoedge.subscriber.service;


import com.telcoedge.domain.Subscriber;
import com.telcoedge.domain.SubscriberStatus;
import com.telcoedge.subscriber.exception.SubscriberAlreadyExistException;
import com.telcoedge.subscriber.exception.SubscriberNotFoundException;
import com.telcoedge.subscriber.persistence.SubscriberEntity;
import com.telcoedge.subscriber.persistence.SubscriberMapper;
import com.telcoedge.subscriber.persistence.SubscriberRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SubscriberService {
    private final SubscriberRepository repository;

    public SubscriberService(SubscriberRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @PreAuthorize("#operatorId == authentication.token.claims['operator_id']")
    public Subscriber create(String operatorId, String msisdn, String name){
        if( repository.existsByOperatorIdAndMsisdn(operatorId, msisdn)){
            throw new SubscriberAlreadyExistException(operatorId, msisdn);
        }
        SubscriberEntity saved = repository.save(
                new SubscriberEntity(operatorId, msisdn, name, SubscriberStatus.ACTIVE)
        );
        return SubscriberMapper.toDomain(saved);
    }

    @PreAuthorize("#operatorId == authentication.token.claims['operator_id']")
    public Subscriber findByMsisdn(String operatorId, String msisdn){
        return repository.findByOperatorIdAndMsisdn(operatorId, msisdn)
                .map(SubscriberMapper::toDomain)
                .orElseThrow(()-> new SubscriberNotFoundException(operatorId, msisdn));
    }
}
