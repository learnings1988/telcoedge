package com.telcoedge.charging;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class SubscriberBalanceLock {
    private final String msisdn;
    private final ReentrantLock lock = new ReentrantLock();
    private BigDecimal balance;

    public SubscriberBalanceLock(String msisdn, BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        lock.lock();
        try{
            return balance;
        }finally {
            lock.unlock();
        }
    }

    public String getMsisdn() {
        return msisdn;
    }

    public boolean deduct(BigDecimal amount){
        lock.lock();
        try{
            if(balance.compareTo(amount)<0) return false;
            balance = balance.subtract(amount);
            return true;
        }finally {
            lock.unlock();
        }
    }
}
