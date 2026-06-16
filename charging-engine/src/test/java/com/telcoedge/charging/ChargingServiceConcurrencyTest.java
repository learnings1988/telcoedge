package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.RepeatedTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ChargingServiceConcurrencyTest {

    /*@RepeatedTest(3)
    void duplicateCdrsShouldBeChargedExactlyOnce() throws InterruptedException{

        BigDecimal initialBalance = new BigDecimal("10000");
        SubscriberBalance balance = new SubscriberBalance("9876543210" , initialBalance);
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        ChargingService service = new ChargingService(new RatingEngine(),
                Map.of("9876543210", balance),
                Map.of("acme", plan)
        );

        List<Cdr> uniqueCdrs = IntStream.range(0,100)
                .mapToObj( i -> new Cdr(UUID.randomUUID(), "acme", "9876543210",
                        UsageType.VOICE, new BigDecimal("100"), Instant.now(),
                        Instant.now())).toList();

        List<Cdr> allSubmissions = new ArrayList<>();

        for(Cdr cdr: uniqueCdrs){
            for(int retry=0; retry<5;retry++){
                allSubmissions.add(cdr);
            }
        }
        Collections.shuffle( allSubmissions);

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ChargeResult>> futures = new ArrayList<>();

        for(Cdr cdr: allSubmissions){
            futures.add( executorService.submit(()->{
                startGate.await();
                return service.process(cdr);
            }));
        }

        startGate.countDown();
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);


        AtomicInteger charged = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for( Future<ChargeResult> future: futures){
            try {
                ChargeResult result = future.get();
                if (result.status() == ChargeStatus.CHARGED) charged.incrementAndGet();
                else if(result.status() == ChargeStatus.DUPLICATE) duplicate.incrementAndGet();
            }catch(ExecutionException e){
                fail("Unexpected Exception: " + e.getCause());
            }
        }

        assertEquals(100, charged.get(), "Exactly 100 CDRs should be charged");
        assertEquals(400, duplicate.get(), "Exactly 100 CDRs should be duplicate");

        BigDecimal expectedBalance = initialBalance.subtract( new BigDecimal("100.0000"));
        assertEquals(0, expectedBalance.compareTo(balance.getBalance()),
                "Balance Mismatch: expected " + expectedBalance + " got " +
                balance.getBalance());
    }

    @RepeatedTest(3)
    void manyConcurrentUniqueCdrsShouldAllCharge() throws InterruptedException{
        BigDecimal initialBalance = new BigDecimal("10000");
        SubscriberBalance balance = new SubscriberBalance("9876543210" , initialBalance);
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        ChargingService service = new ChargingService(new RatingEngine(),
                Map.of("9876543210", balance),
                Map.of("acme", plan)
        );

        int cdrCount = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(cdrCount);


        for(int i=0; i<cdrCount; i++){
            executorService.submit(()->{
                try{
                    startGate.await();
                    Cdr cdr = new Cdr(UUID.randomUUID(), "acme" , "9876543210",
                            UsageType.VOICE, new BigDecimal("10"), Instant.now(), Instant.now());

                    ChargeResult result = service.process(cdr);
                    assertEquals(ChargeStatus.CHARGED, result.status());
                }catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }finally{
                    endGate.countDown();
                }

            });
        }

        startGate.countDown();
        endGate.await();
        executorService.shutdown();

        BigDecimal expectedBalance = initialBalance.subtract( new BigDecimal("100.0000"));
        assertEquals(0, expectedBalance.compareTo(balance.getBalance()),
                "Balance should be: " + expectedBalance + " got " + balance.getBalance());
    }*/
}
