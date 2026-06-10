## Virtual Thread in a real time Charging Engine - What we actually measured

## The Problem
- Telecom Charging: rate CDRs, deduct balance, guarantee idempotency under concurrent load
- Why concurrency matter: same subscriber, multiple CDRs, same millisecond

## What We Built
- Charging Service with per-subscriber ReentrantLock (Not synchronized VT pinning)
- Idempotency via ConcurrentHashMap.putIfAbsent
- Sealed ChargingEvent hierarchy for type-safe event modeling

## How We Measured
- JMH: charging path 0.1 micro second/ops (isolated , no http overhead)
- k6: end to end load test - 50 VUs , 1000 MSISDNs
- JFR: flame graph for hotspot identification
- GC: G1 vs ZGC comparison under load

## Results(Dev Baseline 2-core laptop)
- p95: 64.33ms  p99: 83ms
- Idempotency proof: 1 Charged / 11976 Duplicate
- Hotspot identified: per subscriber lock contention, Jackson JSON, processedEvents Map growth

## Key Decisions and Why
- ReentrantLock over synchronized: VT pinning avoidance
- In-memory map over DB : Phase 1 simplification , DB in Phase2
- ZGC over G1 on dev: empirically better on both latency and throughput(surprising)

## What We'd do Differently in Production
- Replace in-memory maps with DB + Redis cache
- Add TTL eveiction to processedEvents (or move to DB)
- Binary codeds (Protobuf) instead of JSON for internal calls
- Test on 8+ core hardware where ZGC's concurrent work had headroom

## Conclusion
We Proved that latency is not only because of APP E2E test matters http, json etc. other factors also matters for latency
when it comes to performance tuning we cant guess we need to back our decision with numbers as we proved ZGC is actually working better on 2 core laptop
A Simple looking instruction over shared resource can be problemtic under concurrent load