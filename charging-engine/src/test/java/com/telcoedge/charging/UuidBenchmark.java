package com.telcoedge.charging;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@BenchmarkMode({Mode.AverageTime,Mode.Throughput} )
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class UuidBenchmark {

    private final AtomicLong counter = new AtomicLong();

    @Benchmark
    public void uuidRandomUUID(Blackhole bh){
        bh.consume(UUID.randomUUID());
    }

    @Benchmark
    public void uuidThreadLocalRandom(Blackhole bh){
        ThreadLocalRandom r = ThreadLocalRandom.current();
        bh.consume(new UUID(r.nextLong(), r.nextLong()));
    }

    @Benchmark
    public void uuidSequentialCounter(Blackhole bh){
        bh.consume(counter.incrementAndGet());
    }

    public static void main(String[] args) throws Exception{
        Options opt = new OptionsBuilder()
                .include(UuidBenchmark.class.getSimpleName())
                .threads(8)
                .build();
        new Runner(opt).run();
    }

}
