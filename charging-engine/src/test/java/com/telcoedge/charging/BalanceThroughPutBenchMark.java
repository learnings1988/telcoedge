package com.telcoedge.charging;

import org.junit.jupiter.api.Test;

import javax.swing.plaf.TableHeaderUI;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class BalanceThroughPutBenchMark {

    private static final int THREADS = 10;
    private static final int OPS_PER_THREAD = 100000;
    private static final BigDecimal HUGE_BALANCE = new BigDecimal("999999999");
    private static final BigDecimal TINY_DEDUCT = new BigDecimal("0.01");


    private void warmUp() throws InterruptedException{
        SubscriberBalanceAtomic warmUp =
                new SubscriberBalanceAtomic("warmup" , HUGE_BALANCE);
        for(int i = 0; i<50000;i++){
            warmUp.deduct(TINY_DEDUCT);
        }
    }

    private double opsPerSecond(long elapsedMs){
        return ((long)THREADS * OPS_PER_THREAD)/(elapsedMs/1000.0);
    }

    private long runBenchMark(Function<BigDecimal, Boolean> deductFn)
        throws InterruptedException{
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREADS);

        for(int i=0; i<THREADS; i++){
            executor.submit(()->{
                try{
                    startGate.await();
                    for(int j = 0; j<OPS_PER_THREAD; j++){
                        deductFn.apply(TINY_DEDUCT);
                    }
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }finally {
                    endGate.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();
        endGate.await();
        long elapsed = (System.nanoTime()-start)/1000000;
        executor.shutdown();
        return elapsed;
    }


    private long benchmarkSynchronized() throws InterruptedException{
        SubscriberBalance balance = new SubscriberBalance("bench" , HUGE_BALANCE);
        return runBenchMark(balance::deduct);
    }

    private long benchmarkAtomic() throws InterruptedException{
        SubscriberBalanceAtomic balanceAtomic =
                new SubscriberBalanceAtomic("bench", HUGE_BALANCE);
        return runBenchMark(balanceAtomic::deduct);
    }

    private long benchmarkReentrantLock() throws InterruptedException{
        SubscriberBalanceLock balanceLock = new SubscriberBalanceLock("bench", HUGE_BALANCE);
        return runBenchMark(balanceLock::deduct);
    }

    @Test
    void compareAllThreeImplementations() throws InterruptedException{
        System.out.println("\n=== Balance Deduction throughput Benchmark ===");
        System.out.println("Threads: " + THREADS + " | ops/Thread: " + OPS_PER_THREAD
        + " | Total: " + (long)THREADS*OPS_PER_THREAD);
        System.out.println("--------------------------------------------------------");

        warmUp();

        long syncTime = benchmarkSynchronized();
        double syncOps = opsPerSecond(syncTime);

        long atomicTime = benchmarkAtomic();
        double atomicOps = opsPerSecond(atomicTime);

        long lockTime = benchmarkReentrantLock();
        double lockOps = opsPerSecond(lockTime);

        System.out.println("--------------------------------------------------------");
        System.out.printf("Synchronized:    %,10.0f ops/sec    (%d ms)%n",
                syncOps,syncTime);
        System.out.printf("Atomic:    %,10.0f ops/sec    (%d ms)%n",
                atomicOps,atomicTime);
        System.out.printf("Lock:    %,10.0f ops/sec    (%d ms)%n",
                lockOps,lockTime);
        System.out.println("--------------------------------------------------------");

        System.out.printf("CAS VS Synchonized: %.1fx %s%n",
                atomicOps>syncOps? atomicOps/syncOps : syncOps/atomicOps,
                atomicOps>syncOps? "faster CAS wins" : "slower synchronized wins");

        System.out.printf("Lock VS Synchonized: %.1fx %s%n",
                lockOps>syncOps? lockOps/syncOps : syncOps/lockOps,
                lockOps>syncOps? "faster Lock wins" : "slower synchronized wins");

    }
}
