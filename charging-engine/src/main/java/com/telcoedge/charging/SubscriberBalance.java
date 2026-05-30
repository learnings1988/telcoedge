package com.telcoedge.charging;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class SubscriberBalance {
    private final String msisdn;
    private BigDecimal balance;
    private final ReentrantLock lockBalance = new ReentrantLock();
    public SubscriberBalance(String msisdn, BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public BigDecimal getBalance() {
        lockBalance.lock();
        try {
            return balance;
        }finally {
            lockBalance.unlock();
        }
    }

    public boolean deduct( BigDecimal amount) {
        lockBalance.lock();
            try {
                if (balance.compareTo(amount) < 0) return false;
                balance = balance.subtract(amount);
                return true;
            }finally {
                lockBalance.unlock();
            }
    }

    public void credit(BigDecimal amount){
        lockBalance.lock();
        try{
            balance = balance.add(amount);
        }finally {
            lockBalance.unlock();
        }
    }
}
