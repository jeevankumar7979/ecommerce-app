CREATE TABLE payment_record (
    order_id VARCHAR(36) PRIMARY KEY,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    failure_reason VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE payment_outbox (
    id VARCHAR(36) PRIMARY KEY,
    topic VARCHAR(80) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(256),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX payment_outbox_pending_idx ON payment_outbox (created_at) WHERE published = FALSE;
