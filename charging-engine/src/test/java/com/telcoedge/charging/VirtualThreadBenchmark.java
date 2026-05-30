package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.ChargeStatus;
import com.telcoedge.domain.UsageType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualThreadBenchmark {

    private static final int CDR_COUNT = 2000;

    private static final int IO_DELAYS_MS = 10;

    private List<Cdr> generateCdrs(int count){
        List<Cdr> cdrs = new ArrayList<>();
        for(int i=0; i<count; i++){
            cdrs.add(new Cdr(UUID.randomUUID(),
                    "acme","9876543210",
                    UsageType.VOICE,new BigDecimal("60"),
                    Instant.now(), Instant.now()));
        }
        return cdrs;
    }

    private long benchMarkCpuBound(ExecutorService executor,
                                   List<Cdr> cdrs) throws Exception{
        SubscriberBalance balance = new SubscriberBalance("9876543210",
                new BigDecimal("999999999"));
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));

        RatingEngine ratingEngine = new RatingEngine();

        CountDownLatch latch = new CountDownLatch(cdrs.size());
        long startTime = System.nanoTime();

        for(Cdr cdr: cdrs){
            executor.submit(()->{
                try{
                    for(int i=0; i<100; i++){
                        ratingEngine.calculateCharge(cdr, plan);
                    }
                }finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long elapsed = (System.nanoTime()-startTime)/1000000;
        executor.shutdown();
        return elapsed;
    }

    private long benchmarkWithExecutor(ExecutorService executor,
                                       List<Cdr> cdrs, String label) throws Exception{
        SubscriberBalance balance = new SubscriberBalance("9876543210",
                new BigDecimal("999999999"));
        TariffPlan plan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("1.00"));
        ChargingService chargingService = new ChargingService( new RatingEngine(),
                Map.of("9876543210", balance), Map.of("acme", plan));

        CountDownLatch latch = new CountDownLatch(cdrs.size());
        long startTime = System.nanoTime();

        for(Cdr cdr : cdrs){
            executor.submit(()->{
               try{
                   Thread.sleep(IO_DELAYS_MS);
                   ChargeResult result = chargingService.process(cdr);
                   assertEquals(ChargeStatus.CHARGED, result.status() );
               }catch(InterruptedException e){
                   Thread.currentThread().interrupt();
               }finally {
                   latch.countDown();
               }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long elapsed = (System.nanoTime()-startTime)/1000000;
        executor.shutdown();
        if(label != null) System.out.printf(" %-25s -> %d ms%n", label, elapsed);
        return elapsed;
    }

    @Test
    void cpuBoundWorkVTNoAdvantage()throws Exception{

        System.out.println("==== CPU BOUND: VT vs Platform (no) I/O ====");
        int count = 1000;
        List<Cdr> cdrs = generateCdrs(count);

        long platformCpu = benchMarkCpuBound(Executors.newFixedThreadPool(10), cdrs);
        long virtualCpu = benchMarkCpuBound(Executors.newVirtualThreadPerTaskExecutor(), cdrs);

        System.out.printf("Platform(10 Threads): %d ms%n", platformCpu);
        System.out.printf("Virtual Threads: %d ms%n", virtualCpu);
        System.out.printf("Difference: %.1fx (should be ~1.0x - VT has no advantage for CPU work)%n",
                (double) platformCpu/virtualCpu);

        System.out.println("==================================================================");
    }

    @Test
    void scalingTestIncreasingConcurrency() throws Exception{

        System.out.println("==== scaling: How VT handles increasing Load ====" );
        System.out.println("Simulated I/O " + IO_DELAYS_MS + " ms per CDR");
        System.out.println("----------------------------------------------------");

        int[] loads = {100, 500, 1000, 5000, 10000};

        for(int load: loads){
            List<Cdr> cdrs = generateCdrs(load);
            long vtTime = benchmarkWithExecutor( Executors.newVirtualThreadPerTaskExecutor(),
                    cdrs, null);

            double throughput = load/(vtTime/1000.0);
            System.out.printf(" %5d CDRs -> %4d ms | %,.0f CDRs/sec%n", load, vtTime, throughput);
            System.out.println("----------------------------------------------------------------");
        }
    }


    @Test
    void platformThreadVsVirtualThreads() throws Exception{
        List<Cdr> cdrs = generateCdrs(CDR_COUNT);
        System.out.println("\n==== Platform thread vs virtual threads ====");
        System.out.println(" CDRs: " + CDR_COUNT + " | Simulated I/O " + IO_DELAYS_MS +
                "ms per second");

        System.out.println("----------------------------------------------------------------");

        long platformTime = benchmarkWithExecutor(Executors.newFixedThreadPool(200),
                cdrs, "Platform (200 Threads)");

        long vtTime = benchmarkWithExecutor( Executors.newVirtualThreadPerTaskExecutor(), cdrs,
                "Virtual Threads");

        long platform50Time = benchmarkWithExecutor(Executors.newFixedThreadPool(50),
                cdrs, "Platform (50 Threads)");

        System.out.println("----------------------------------------------------------------");
        System.out.printf("Platform 200 Threads: %d ms%n", platformTime);
        System.out.printf("Platform 50 Threads: %d ms%n", platform50Time);
        System.out.printf("Virtual Threads: %d ms%n", vtTime);
        System.out.printf("VT speedup vd 200-pool: %.1fx%n", (double) platformTime/vtTime);
        System.out.printf("VT speedup vd 50-pool: %.1fx%n", (double) platform50Time/vtTime);

        System.out.println("----------------------------------------------------------------");
    }

    /*@Test
    void verifyPinningDetection() throws InterruptedException {
        System.out.println("Vendor: " + System.getProperty("java.vendor"));
        System.out.println("Version: " + System.getProperty("java.version"));
        System.out.println("tracePinnedThreads: " + System.getProperty("jdk.tracePinnedThreads"));

        Thread.startVirtualThread(()->{
            synchronized (this){
                try{
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }).join();
    }*/

}
