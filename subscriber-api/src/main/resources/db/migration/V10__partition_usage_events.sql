ALTER TABLE usage_events RENAME TO usage_events_old;

DROP INDEX IF EXISTS idx_usage_event_subscriber_time;
DROP INDEX IF EXISTS idx_usage_events_operator_time;
DROP INDEX IF EXISTS idx_usage_event_sub_type_time;


CREATE TABLE usage_events(
                             id              BIGINT       GENERATED ALWAYS AS IDENTITY,
                             event_id        UUID            NOT NULL ,
                             subscriber_id   BIGINT          NOT NULL REFERENCES subscribers(ID),
                             operator_id     VARCHAR(64)     NOT NULL REFERENCES operators(code),
                             usage_type      VARCHAR(10)     NOT NULL CHECK ( usage_type IN ('VOICE', 'DATA', 'SMS')),
                             units           NUMERIC(12,2)   NOT NULL,
                             rate_applied    NUMERIC(10,4)   NOT NULL,
                             amount_charged  NUMERIC(15,4)   NOT NULL,
                             balance_after   NUMERIC(15,4)   NOT NULL,
                             status          VARCHAR(30)     NOT NULL
                                 CHECK ( status IN ('CHARGED','INSUFFICIENT_BALANCE','SUBSCRIBER_NOT_FOUND','DUPLICATE' )),
                             processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                             PRIMARY KEY (id , processed_at),
                             UNIQUE(event_id, processed_at)

) PARTITION BY RANGE (processed_at);

CREATE TABLE usage_events_2026_07 PARTITION OF usage_events
FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE usage_events_2026_08 PARTITION OF usage_events
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE TABLE usage_events_default PARTITION OF usage_events DEFAULT;

INSERT INTO usage_events (event_id, subscriber_id, operator_id, usage_type, units, rate_applied, amount_charged, balance_after, status, processed_at)
SELECT event_id, subscriber_id, operator_id, usage_type, units, rate_applied, amount_charged, balance_after, status, processed_at
FROM usage_events_old;

DROP TABLE usage_events_old;

CREATE INDEX idx_usage_event_subscriber_time
    ON usage_events(subscriber_id, processed_at DESC);

CREATE INDEX idx_usage_events_operator_time
    ON usage_events(operator_id, processed_at DESC);

CREATE INDEX idx_usage_event_sub_type_time
    ON usage_events (subscriber_id, usage_type, processed_at desc );

ALTER TABLE usage_events_2026_07 SET(
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
    );

ALTER TABLE usage_events_2026_08 SET(
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
    );

ALTER TABLE usage_events_default SET(
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
    );