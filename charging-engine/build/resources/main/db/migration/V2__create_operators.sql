CREATE TABLE operators(
    id          BIGSERIAL       PRIMARY KEY,
    code        VARCHAR(64)     NOT NULL UNIQUE,
    name        VARCHAR(100)    NOT NULL,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ      NOT NULL DEFAULT now()
);

INSERT INTO operators (code, name) VALUES ('acme' , 'Acme Telecom');

ALTER TABLE subscribers
    ADD CONSTRAINT fk_subscribers_operator FOREIGN KEY (operator_id) references operators(code);


DO $$
BEGIN
    IF NOT EXISTS(
      select 1 from information_schema.table_constraints
      where table_name = 'subscribers'
      and constraint_name = 'subscribers_status_check'
    )THEN
      ALTER TABLE subscribers
      ADD CONSTRAINT subscribers_status_check
      CHECK ( status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'));
    END IF;
END
$$;