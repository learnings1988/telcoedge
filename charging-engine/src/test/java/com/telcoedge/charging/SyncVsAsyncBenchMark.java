package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SyncVsAsyncBenchMark {

    /*private static final int CDR_COUNT = 500;

    @Test
    void syncVSAsyncThroughput(){

        SubscriberBalance balance = new SubscriberBalance("9876543210",
                new BigDecimal("999999999"));
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        ChargingService syncService = new ChargingService(new RatingEngine(),
                Map.of("9876543210", balance),
                Map.of("acme", plan)
        );

        ExecutorService executor = Executors.newFixedThreadPool(20);
        SubscriberBalance asyncBalance = new SubscriberBalance("9876543210",
                new BigDecimal("999999999"));
        AsyncChargingService asyncChargingService = new AsyncChargingService(
                new RatingEngine(),
                Map.of("9876543210", asyncBalance),
                Map.of("acme",plan),executor);

        List<Cdr> cdrs = new ArrayList<>();
        for(int i=0; i<CDR_COUNT; i++){
            cdrs.add(new Cdr(UUID.randomUUID(), "acme", "9876543210",
                    UsageType.VOICE, new BigDecimal("10"), Instant.now(),
                    Instant.now()));
        }

        long syncStart = System.nanoTime();
        for(Cdr cdr: cdrs){
            syncService.process(cdr);
        }
        long syncElapsed = (System.nanoTime()-syncStart)/1000000;


        long asyncStart = System.nanoTime();
        List<CompletableFuture<ChargeResult>> futures = cdrs.stream()
                .map(asyncChargingService::processAsync)
                .toList();
        CompletableFuture.allOf( futures.toArray(new CompletableFuture[0])).join();
        long asyncElapsed = (System.nanoTime()-asyncStart)/1000000;
        executor.shutdown();


        System.out.println("\n=== Sync vs Async Benchmark (with Simulated I/O) ===");
        System.out.println("CDRs processed: " + CDR_COUNT);
        System.out.println("Sync (sequential): " + syncElapsed + " ms");
        System.out.println("ASync (concurrent): " + asyncElapsed + " ms");
        System.out.println("=======================");
    }*/
}
