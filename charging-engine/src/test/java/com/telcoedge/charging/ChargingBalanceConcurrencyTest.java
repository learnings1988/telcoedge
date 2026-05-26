package com.telcoedge.charging;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChargingBalanceConcurrencyTest {

    @RepeatedTest(5)
    void concurrentDeductionsShouldNotLoseMoney() throws InterruptedException{
        SubscriberBalance subscriberBalance = new SubscriberBalance("9876543210",
                new BigDecimal("1000"));

        int threadCount =1000;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch( threadCount);

        for(int i=0; i<threadCount; i++){
            executorService.submit(()->{
                try{
                    startGate.await();
                    subscriberBalance.deduct((new BigDecimal("1")));
                }catch( InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally{
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executorService.shutdown();

        assertEquals( 0,
                subscriberBalance.getBalance().compareTo(new BigDecimal(0)),
                "Balance should be exactly 0 but was " + subscriberBalance.getBalance()+
                 " - lost " + subscriberBalance.getBalance() + " Worth of deductions");
    }

    @Test
    void measureDeductionThroughPut() throws InterruptedException{
        SubscriberBalance balance = new SubscriberBalance("9876543210",
                new BigDecimal("10000000"));

        int threadCount = 10;
        int operationsPerThread = 100_000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch( threadCount);

        long startTime = System.nanoTime();

        for(int i=0; i<threadCount; i++){
            executorService.submit(()->{
                try{
                    startGate.await();
                    for(int j=0; j<operationsPerThread; j++){
                        balance.deduct(new BigDecimal("0.01"));
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executorService.shutdown();

        long elapsed = System.nanoTime() - startTime;
        long totalOps = (long) threadCount * operationsPerThread;
        double opsPerSecond = totalOps/(elapsed/1000000000.0);

        System.out.println("=== Throughput Measurement ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Total Operations: " + totalOps);
        System.out.println("Elapsed: " + (elapsed/1000000) + " ms");
        System.out.printf("Throughput: %.0f ops/sec%n", opsPerSecond);
        System.out.println("Final balance: " + balance.getBalance());

        BigDecimal expected = new BigDecimal("10000000").
                subtract( new BigDecimal("0.01").multiply(new BigDecimal(totalOps)));

        assertEquals(0, balance.getBalance().compareTo( expected),
                "Balance mismatch - synchronization broker under sustained load");
    }
}
