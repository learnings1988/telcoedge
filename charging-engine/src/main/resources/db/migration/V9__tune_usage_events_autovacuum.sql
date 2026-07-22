ALTER TABLE usage_events SET(
    autovacuum_vacuum_scale_factor=0.01,
    autovacuum_analyze_scale_factor=0.005
    );

ALTER TABLE idempotency_keys SET(
    autovacuum_vacuum_scale_factor=0.02,
    autovacuum_analyze_scale_factor=0.01
    );