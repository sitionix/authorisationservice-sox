CREATE TABLE IF NOT EXISTS forge_outbox_events (
    id                BIGSERIAL PRIMARY KEY,
    event_type        VARCHAR(255) NOT NULL,
    payload           TEXT         NOT NULL,
    headers           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    trace_id          VARCHAR(255),
    aggregate_type_id BIGINT,
    aggregate_type    VARCHAR(255),
    aggregate_id      BIGINT,
    status_id         BIGINT       NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    initiator_type_id BIGINT,
    initiator_type    VARCHAR(255),
    initiator_id      VARCHAR(64),
    retry_count       INT          NOT NULL,
    next_retry_at     TIMESTAMPTZ  NOT NULL,
    last_error        TEXT,
    lock_until        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_forge_outbox_events_aggregate_type_id
        FOREIGN KEY (aggregate_type_id) REFERENCES forge_outbox_aggregate_types (id),
    CONSTRAINT fk_forge_outbox_events_status_id
        FOREIGN KEY (status_id) REFERENCES forge_outbox_statuses (id),
    CONSTRAINT fk_forge_outbox_events_initiator_type_id
        FOREIGN KEY (initiator_type_id) REFERENCES forge_outbox_initiator_types (id)
);
