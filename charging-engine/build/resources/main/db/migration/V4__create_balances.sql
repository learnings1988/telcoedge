CREATE TABLE balances (
    id              BIGSERIAL       PRIMARY KEY,
    subscriber_id   BIGINT          NOT NULL REFERENCES subscribers(ID),
    amount          NUMERIC(15,4)   NOT NULL DEFAULT 0,
    version         INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_balance_subscriber UNIQUE (subscriber_id)
);