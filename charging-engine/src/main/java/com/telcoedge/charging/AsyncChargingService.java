package com.telcoedge.charging;

import ch.qos.logback.core.joran.conditional.ThenAction;
import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import org.springframework.dao.ConcurrencyFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class AsyncChargingService {

    private final RatingEngine ratingEngine;
    private final Map<String, SubscriberBalance> balances;
    private final Map<String, TariffPlan> tariffPlans;
    private final ConcurrentHashMap<UUID, ChargeResult> processedEvents;
    private final Executor executor;

    public AsyncChargingService(RatingEngine ratingEngine,
                                Map<String, SubscriberBalance> balances,
                                Map<String, TariffPlan> tariffPlans,
                                Executor executor) {
        this.ratingEngine = ratingEngine;
        this.balances = balances;
        this.tariffPlans = tariffPlans;
        this.executor = executor;
        this.processedEvents = new ConcurrentHashMap<>();
    }


    private void simulateIo(int millis){
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private ChargeResult buildResult(Cdr cdr, BigDecimal charged,
                                     BigDecimal remainingBalance,
                                     ChargeStatus status){
        return new ChargeResult( cdr.eventId(), cdr.msisdn(), charged,
                remainingBalance, status, Instant.now());
    }

    private ChargeResult executeCharge(Cdr cdr, SubscriberBalance balance,
                                       TariffPlan plan){
        BigDecimal chargedAmount = ratingEngine.calculateCharge(cdr, plan);
        boolean success = balance.deduct(chargedAmount);

        if(!success){
            return buildResult(cdr, chargedAmount, balance.getBalance(),
                    ChargeStatus.INSUFFICIENT_BALANCE);
        }

        ChargeResult result = buildResult(cdr, chargedAmount, balance.getBalance(),
                ChargeStatus.CHARGED);

        ChargeResult race = processedEvents.putIfAbsent(cdr.eventId(), result);

        if(race !=null){
            balance.credit(chargedAmount);
            return buildResult(cdr, race.amountCharged(), race.remainingBalance(),
                    ChargeStatus.DUPLICATE);
        }

        return result;
    }

    private CompletableFuture<TariffPlan> lookupTariff(Cdr cdr){
        return CompletableFuture.supplyAsync(()->{
            simulateIo(2);
            TariffPlan plan= tariffPlans.get(cdr.operatorId());

            if(plan == null) throw new IllegalStateException("No Tariff plan for operatorId: "+
                    cdr.operatorId());
            return plan;
        }, executor);
    }

    private CompletableFuture<Object> lookUpSubscriber(Cdr cdr){
        return CompletableFuture.supplyAsync(()->{
            simulateIo(3);
            SubscriberBalance balance = balances.get(cdr.msisdn());

            if(balance==null){
                return buildResult(cdr, BigDecimal.ZERO, BigDecimal.ZERO,
                        ChargeStatus.SUBSCRIBER_NOT_FOUND);
            }
            return balance;
        }, executor);
    }

    private CompletableFuture<ChargeResult> checkDuplicate( Cdr cdr){
        return CompletableFuture.supplyAsync(()->{
            simulateIo(2);
            ChargeResult existing = processedEvents.get(cdr.eventId());
            if(existing != null){
                return buildResult(cdr, existing.amountCharged(),
                        existing.remainingBalance(), ChargeStatus.DUPLICATE);
            }
            return null;
        }, executor);
    }

    public CompletableFuture<ChargeResult> processAsync(Cdr cdr){

        return checkDuplicate(cdr).
                thenCompose(duplicate -> {
                    if(duplicate != null) return CompletableFuture.completedFuture(duplicate);
                    return lookUpSubscriber(cdr);
                }).thenCompose(balanceOrResult -> {
                    if(balanceOrResult instanceof ChargeResult result){
                        return CompletableFuture.completedFuture(result);
                    }
                    SubscriberBalance balance = (SubscriberBalance) balanceOrResult;
                    return lookupTariff(cdr)
                            .thenApply( plan -> executeCharge(cdr,
                                    balance, plan));
                }).exceptionally(ex-> buildResult(cdr, BigDecimal.ZERO,
                        BigDecimal.ZERO, ChargeStatus.SUBSCRIBER_NOT_FOUND));
    }
}
