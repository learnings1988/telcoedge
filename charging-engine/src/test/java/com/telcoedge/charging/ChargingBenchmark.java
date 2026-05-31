package com.telcoedge.charging;

import com.telcoedge.domain.Cdr;
import com.telcoedge.domain.ChargeResult;
import com.telcoedge.domain.UsageType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class ChargingBenchmark {

    private RatingEngine ratingEngine;
    private TariffPlan tariffPlan;
    private Cdr voiceCdr;
    private Cdr dataCdr;
    private SubscriberBalance balance;

    @Setup(Level.Trial)
    public void setup(){
        ratingEngine = new RatingEngine();
        tariffPlan = new TariffPlan("basic", "Basic",
                new BigDecimal("0.01"), new BigDecimal("0.50"),
                new BigDecimal("1.00"));
        balance = new SubscriberBalance("9876543210",
                new BigDecimal("999999"));
    }

    @Setup(Level.Invocation)
    public void setupPerInvocation(){
        voiceCdr = new Cdr(UUID.randomUUID(),"acme","9876543210",
                UsageType.VOICE, new BigDecimal("120"), Instant.now(),
                Instant.now());
        dataCdr = new Cdr(UUID.randomUUID(),"acme","9876543210",
                UsageType.DATA, new BigDecimal("50"), Instant.now(),
                Instant.now());
    }

    @Benchmark
    public void rateVoiceCdr(Blackhole bh){
        BigDecimal charge = ratingEngine.calculateCharge(voiceCdr, tariffPlan);
        bh.consume(charge);
    }


    @Benchmark
    public void rateDataCdr(Blackhole bh){
        BigDecimal charge = ratingEngine.calculateCharge(dataCdr, tariffPlan);
        bh.consume(charge);
    }

    @Benchmark
    public void deductBalance(Blackhole bh){
        boolean result = balance.deduct(new BigDecimal("1.20"));
        bh.consume(result);
    }

    @Benchmark
    public void fullPipelineRateAndDeduct(Blackhole bh){
        BigDecimal charge = ratingEngine.calculateCharge(voiceCdr, tariffPlan);
        boolean result = balance.deduct(charge);
        bh.consume(result);
    }


    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ChargingBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
