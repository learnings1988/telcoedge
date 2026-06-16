CREATE TABLE plans(
    id          BIGSERIAL       PRIMARY KEY,
    operator_id VARCHAR(64)     NOT NULL REFERENCES operators(code),
    name        VARCHAR(100)    NOT NULL ,
    description TEXT,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE tariff_rates(
    id              BIGSERIAL       PRIMARY KEY,
    plan_id         BIGINT          NOT NULL  REFERENCES plans(id),
    usage_type      VARCHAR(10)     NOT NULL CHECK ( usage_type IN ('VOICE', 'DATA', 'SMS')),
    rate_per_unit   NUMERIC(10,4)   NOT NULL ,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tariff_plan_usage UNIQUE (plan_id, usage_type)
);

CREATE TABLE subscriber_plans(
    id              BIGSERIAL       PRIMARY KEY,
    subscriber_id   BIGSERIAL       NOT NULL REFERENCES subscribers(id),
    plan_id         BIGSERIAL       NOT NULL REFERENCES plans(id),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    activated_at    TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    deactivated_at  TIMESTAMPTZ
);


CREATE INDEX idx_subscriber_plans_active
    ON subscriber_plans(subscriber_id) WHERE active=TRUE;

INSERT INTO plans(operator_id, name, description)
    VALUES ('acme', 'Basic Plan', 'Standard prepaid plan with voice data and sms');

INSERT INTO tariff_rates( plan_id, usage_type, rate_per_unit)
    VALUES (1, 'VOICE', '0.0100'),
           (1, 'DATA', '0.5000'),
           (1,'SMS','1.000');