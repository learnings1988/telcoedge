package com.telcoedge.charging;

import com.telcoedge.charging.persistence.*;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


/**
 * Phase 1 Dev Baseline(2 core laptop, 50VUs, 1000 MSISDNs)
 * G1: p99 ~116ms, throughput ~1134 req/s
 * ZGC: p99 ~83ms, throughput ~1285 req/s
 * Productiont target: 1K TPS, p99<50ms -- requires prod hardware + phase 2 Persistence
 * Hotspots: per-subscriber ReentrantLock, Jackson JSON, unbounded processedEvents map.
 */

@Service
public class ChargingService {


    private static final Logger log = LoggerFactory.getLogger(ChargingService.class);
    private final RatingEngine ratingEngine;
    private final BalanceRepository balanceRepository;
    private final UsageEventRepository usageEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TariffRateRepository tariffRateRepository;
    private final SubscriberLookup subscriberLookup;

    public ChargingService(RatingEngine ratingEngine, BalanceRepository balanceRepository,
                           UsageEventRepository usageEventRepository,
                           IdempotencyKeyRepository idempotencyKeyRepository,
                           TariffRateRepository tariffRateRepository,
                           SubscriberLookup subscriberLookup) {
        this.ratingEngine = ratingEngine;
        this.balanceRepository = balanceRepository;
        this.usageEventRepository = usageEventRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.tariffRateRepository = tariffRateRepository;
        this.subscriberLookup = subscriberLookup;
    }

    @Transactional
    public ChargeResult process(Cdr cdr){
        Optional<IdempotencyKeyEntity> existingKey =
                idempotencyKeyRepository.findByEventId(cdr.eventId());
        if(existingKey.isPresent()){
            log.debug("Duplicate event with eventId : {} Detected", cdr.eventId());
            return buildResult(cdr, BigDecimal.ZERO, BigDecimal.ZERO,
                    ChargeStatus.DUPLICATE);
        }

        Long subscriberId = subscriberLookup.findSubscriberId(cdr.operatorId(), cdr.msisdn());
        if(subscriberId==null){
            return buildResult(cdr, BigDecimal.ZERO, BigDecimal.ZERO,
                    ChargeStatus.SUBSCRIBER_NOT_FOUND);
        }

        List<TariffRateView> rates = tariffRateRepository.findActiveRatesForSubscriber(subscriberId);

        BigDecimal rate = findRateForUsageType(cdr.usageType(), rates);

        BigDecimal chargedAmount = ratingEngine.calculateCharge(cdr.usageType(),
                cdr.quantity(), rate);

        return doCharge(cdr, subscriberId, chargedAmount, rate);
    }


    private ChargeResult doCharge(Cdr cdr, Long subscriberId, BigDecimal chargeAmount, BigDecimal rate){
        BalanceEntity balance = balanceRepository.findBySubscriberId(subscriberId).
                orElseThrow(()-> new IllegalStateException(
                        STR."No balance recordfor subscriber : \{subscriberId}"));

        boolean success = balance.deduct(chargeAmount);
        if(!success){
            return buildResult(cdr, chargeAmount, balance.getAmount(),
                    ChargeStatus.INSUFFICIENT_BALANCE);
        }

        UsageEventEntity usageEventEntity = new UsageEventEntity(
                cdr.eventId(), subscriberId, cdr.operatorId(), cdr.usageType(),
                cdr.quantity(), rate, chargeAmount, balance.getAmount(), "CHARGED",
                Instant.now()
        );

        usageEventRepository.save(usageEventEntity);

        IdempotencyKeyEntity key = new IdempotencyKeyEntity(cdr.eventId(), "CHARGED");

        idempotencyKeyRepository.save(key);

        return buildResult(cdr, chargeAmount, balance.getAmount(), ChargeStatus.CHARGED);
    }

    private BigDecimal findRateForUsageType(UsageType usageType, List<TariffRateView> rates){
        return rates.stream()
                .filter( r -> r.getUsageType() == usageType)
                .map(TariffRateView::getRatePerUnit)
                .findFirst()
                .orElse(null);
    }

    private ChargeResult buildResult(Cdr cdr, BigDecimal charged,
                                     BigDecimal remaining, ChargeStatus status){
        return new ChargeResult(cdr.eventId(), cdr.msisdn(), charged, remaining, status, Instant.now());
    }
}
