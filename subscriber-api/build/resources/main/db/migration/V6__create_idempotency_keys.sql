CREATE TABLE idempotency_keys(
    id              BIGSERIAL       PRIMARY KEY,
    event_id        UUID            NOT NULL UNIQUE,
    status          VARCHAR(30)     NOT NULL ,
    response_hash   VARCHAR(64),
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ      NOT NULL DEFAULT (NOW() + INTERVAL '24 hours')
);

CREATE INDEX idx_idempotency_keys_expires
    ON idempotency_keys(expires_at);
