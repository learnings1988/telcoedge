CREATE TABLE usage_events(
    id              BIGSERIAL       PRIMARY KEY,
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

    CONSTRAINT uq_usage_event_id UNIQUE (event_id)

);

CREATE INDEX idx_usage_event_subscriber_time
    ON usage_events(subscriber_id, processed_at DESC);

CREATE INDEX idx_usage_events_operator_time
    ON usage_events(operator_id, processed_at DESC);