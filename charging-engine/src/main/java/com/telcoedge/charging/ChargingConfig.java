package com.telcoedge.charging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ChargingConfig {

    @Bean
    public RatingEngine ratingEngine(){
        return new RatingEngine();
    }

    @Bean
    public Map<String, SubscriberBalance> subscriberBalances(){
        Map<String, SubscriberBalance> balances = new ConcurrentHashMap<>();

        for(int i=0; i<1000; i++){
            String msisdn = "987654" + String.format("%04d",i);
            balances.put(msisdn, new SubscriberBalance( msisdn,
                    new BigDecimal("999999.00")));
        }
        return balances;
    }

    @Bean
    public Map<String, TariffPlan> tariffPlans(){
        Map<String, TariffPlan> plans = new ConcurrentHashMap<>();
        plans.put("acme", new TariffPlan("basic","Basic Plan",
                new BigDecimal("0.01"),
                new BigDecimal("0.50"),
                new BigDecimal("1.00")));
        return plans;
    }

    @Bean
    public ChargingService chargingService(RatingEngine ratingEngine,
                                           Map<String, SubscriberBalance> subscriberBalances,
                                           Map<String, TariffPlan> tariffPlans ){
        return new ChargingService(ratingEngine, subscriberBalances,tariffPlans);
    }
}
