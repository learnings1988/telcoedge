CREATE TABLE subscribers (
    id              BIGSERIAL PRIMARY KEY,
    operator_id     VARCHAR(64) NOT NULL,
    msisdn          VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT subscribers_status_check CHECK (status IN ('ACTIVE', 'TERMINATED', 'SUSPENDED'))
);

CREATE UNIQUE INDEX idx_subscribers_operator_msisdn
    ON subscribers (operator_id, msisdn);

CREATE INDEX idx_subscribers_operator_status
    ON subscribers (operator_id, status)
    WHERE status = 'ACTIVE';
