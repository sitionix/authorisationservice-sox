CREATE TABLE IF NOT EXISTS outbox_events (
    id                BIGSERIAL PRIMARY KEY,
    aggregate_type_id BIGINT NOT NULL,
    aggregate_id      BIGINT NOT NULL,
    event_type_id     BIGINT NOT NULL,
    status_id         BIGINT NOT NULL,
    initiator_type_id BIGINT NOT NULL DEFAULT 2,
    initiator_id      VARCHAR(64),
    retry_count       INT NOT NULL,
    next_retry_at     TIMESTAMPTZ NOT NULL,
    payload           JSONB NOT NULL,
    last_error        TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
