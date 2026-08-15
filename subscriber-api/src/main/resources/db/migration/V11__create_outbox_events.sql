CREATE TABLE outbox_events(
    id                  BIGSERIAL       PRIMARY KEY,
    aggregate_type      VARCHAR(64)     NOT NULL ,
    aggregate_id        VARCHAR(64)     NOT NULL,
    event_type          VARCHAR(64)     NOT NULL,
    payload             TEXT            NOT NULL,
    create_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published           BOOLEAN         NOT NULL DEFAULT false,
    published_at        TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
ON outbox_events (create_at asc) where published = false;