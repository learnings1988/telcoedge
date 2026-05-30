package com.telcoedge.charging;


import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProfilingLoadTest {

    private static final int DURATION_SECONDS = 10;
    private static final int THREAD_COUNT = 50;

    @Test
    void sustainedLoadForProfiling() throws InterruptedException{

        SubscriberBalance balance = new SubscriberBalance("9876543210", new BigDecimal("1000000"));
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        ChargingService service = new ChargingService(new RatingEngine(),
                Map.of("9876543210", balance),
                Map.of("acme", plan)
        );

        AtomicInteger totalProcessed = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        AtomicLong totalLatencyNanos = new AtomicLong();

        Instant deadline = Instant.now().plus(Duration.ofSeconds(DURATION_SECONDS));

        try(var executor =  Executors.newVirtualThreadPerTaskExecutor()){
            for(int t=0; t<THREAD_COUNT; t++){
                executor.submit(()->{
                    while(Instant.now().isBefore(deadline)){
                        Cdr cdr = new Cdr(UUID.randomUUID(),"acme",
                                "9876543210", UsageType.VOICE,
                                new BigDecimal("60"),
                                Instant.now(), Instant.now());

                        long start = System.nanoTime();
                        ChargeResult result = service.process(cdr);
                        long elapsed = System.nanoTime() - start;

                        totalLatencyNanos.addAndGet(elapsed);

                        if(result.status()== ChargeStatus.CHARGED){
                            totalProcessed.incrementAndGet();
                        }else{
                            error.incrementAndGet();
                        }
                    }
                });
            }
            Thread.sleep((DURATION_SECONDS+2) * 1000L);

            double avgLatencyUs = (totalLatencyNanos.get() /
                    (double) totalProcessed.get())/1000.0;

            double throughput = totalProcessed.get()/(double) DURATION_SECONDS;

            System.out.println("\n==== Sustained Load Profile ====");
            System.out.println("Duration: " + DURATION_SECONDS + "s");
            System.out.println("Virtual Threads: " + THREAD_COUNT);
            System.out.println("total CDRS processed: " + totalProcessed.get());
            System.out.println(" total errors: " + error.get());
            System.out.printf("Through Put : %,.0f Cdrs/sec%n" , throughput);
            System.out.printf("Avg Latency: %.1f us%n", avgLatencyUs);
            System.out.println("\n======================================");
        }
    }
}
