package com.telcoedge.charging;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriberBalanceAtomic {

    private final String msisdn;
    private final AtomicReference<BigDecimal> balance;

    public SubscriberBalanceAtomic(String msisdn,
                                   BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = new AtomicReference<> (balance);
    }

    public String getMsisdn() {
        return msisdn;
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    public boolean deduct(BigDecimal amount){
        while (true){
            BigDecimal current = balance.get();
            if(current.compareTo(amount)<0) return false;
            BigDecimal next = current.subtract(amount);
            if(balance.compareAndSet(current, next)){
                return true;
            }
        }
    }
}

