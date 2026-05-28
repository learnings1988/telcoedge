package com.telcoedge.charging;

import java.math.BigDecimal;

public class SubscriberBalance {
    private final String msisdn;
    private BigDecimal balance;
    private final Object lockDeduct = new Object();
    public SubscriberBalance(String msisdn, BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public BigDecimal getBalance() {
        synchronized (lockDeduct) {
            return balance;
        }
    }

    public boolean deduct( BigDecimal amount) {
        synchronized (lockDeduct) {
            if (balance.compareTo(amount) < 0) return false;
            balance = balance.subtract(amount);
            return true;
        }
    }

    public void credit(BigDecimal amount){
        synchronized (lockDeduct){
            balance = balance.add(amount);
        }
    }
}
