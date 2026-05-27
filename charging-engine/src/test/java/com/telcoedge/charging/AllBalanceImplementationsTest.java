package com.telcoedge.charging;

import org.junit.jupiter.api.RepeatedTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AllBalanceImplementationsTest {

    private static final int THREAD_COUNT = 1000;
    private static final BigDecimal INITIAL = new BigDecimal(1000);
    private static final BigDecimal DEDUCT_AMOUNT = new BigDecimal(1);

    @RepeatedTest(3)
    void synchronizedVersion_isCorrectUnderContention() throws InterruptedException{
        SubscriberBalance balance = new SubscriberBalance("9876543210" , INITIAL);
        runConcurrentDeductions(balance::deduct);
        assertEquals(0, balance.getBalance().compareTo(BigDecimal.ZERO),
                "Synchronized: Expected 0, got" + balance.getBalance());
    }


    @RepeatedTest(3)
    void atomicVersion_isCorrectUnderContention() throws InterruptedException{
        SubscriberBalanceAtomic balance = new SubscriberBalanceAtomic("9876543210" , INITIAL);
        runConcurrentDeductions(balance::deduct);
        assertEquals(0, balance.getBalance().compareTo(BigDecimal.ZERO),
                "Atomic: Expected 0, got" + balance.getBalance());
    }

    @RepeatedTest(3)
    void reentrantLockVersion_isCorrectUnderContention() throws InterruptedException{
        SubscriberBalanceLock balance = new SubscriberBalanceLock("9876543210" , INITIAL);
        runConcurrentDeductions(balance::deduct);
        assertEquals(0, balance.getBalance().compareTo(BigDecimal.ZERO),
                "Atomic: Expected 0, got" + balance.getBalance());
    }

    private void runConcurrentDeductions(Function<BigDecimal, Boolean> deductFn)
            throws InterruptedException{
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREAD_COUNT);

        for(int i=0; i<THREAD_COUNT; i++){
            executor.submit(()->{
                try{
                    startGate.await();
                    deductFn.apply(DEDUCT_AMOUNT);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executor.shutdown();
    }
}
