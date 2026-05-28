package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChargingService {


    private final RatingEngine ratingEngine;
    private final Map<String, SubscriberBalance> balances;
    private final Map<String, TariffPlan> tariffPlanMap;
    private final ConcurrentHashMap<UUID, ChargeResult> processedEvents;

    public ChargingService(RatingEngine ratingEngine,
                           Map<String, SubscriberBalance> balances,
                           Map<String, TariffPlan> tariffPlanMap) {
        this.ratingEngine = ratingEngine;
        this.balances = balances;
        this.tariffPlanMap = tariffPlanMap;
        this.processedEvents = new ConcurrentHashMap<>();
    }

    public ChargeResult process(Cdr cdr){
        ChargeResult existing = processedEvents.get(cdr.eventId());
        if(existing!=null){
            return buildResult(cdr, existing.amountCharged(), existing.remainingBalance(),
                    ChargeStatus.DUPLICATE);
        }

        SubscriberBalance subscriberBalance = balances.get( cdr.msisdn());
        if(subscriberBalance == null){
            return buildResult(cdr, BigDecimal.ZERO, BigDecimal.ZERO, ChargeStatus.SUBSCRIBER_NOT_FOUND);
        }

        TariffPlan plan = tariffPlanMap.get(cdr.operatorId());
        if(plan == null){
            return buildResult(cdr, BigDecimal.ZERO, subscriberBalance.getBalance(),
                    ChargeStatus.SUBSCRIBER_NOT_FOUND);
        }

        BigDecimal chargedAmount = ratingEngine.calculateCharge(cdr, plan);
        boolean success = subscriberBalance.deduct(chargedAmount);

        if(!success){
            return buildResult(cdr, chargedAmount, subscriberBalance.getBalance(),
                    ChargeStatus.INSUFFICIENT_BALANCE);
        }

        ChargeResult result = buildResult( cdr, chargedAmount, subscriberBalance.getBalance(),
                ChargeStatus.CHARGED);

        ChargeResult race = processedEvents.putIfAbsent( cdr.eventId(), result);
        if(race != null){
            subscriberBalance.credit(chargedAmount);
            return buildResult(cdr, race.amountCharged(), race.remainingBalance(),
                    ChargeStatus.DUPLICATE);
        }
        return result;
    }

    private ChargeResult buildResult(Cdr cdr, BigDecimal charged, BigDecimal remaining,
                                     ChargeStatus status){
        return new ChargeResult(cdr.eventId(), cdr.msisdn(), charged, remaining, status, Instant.now());
    }
}
