CREATE TABLE events.idempotency_keys (
    id UUID PRIMARY KEY,
    organizer_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL REFERENCES events.events (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_idempotency_organizer_key UNIQUE (organizer_id, idempotency_key)
);
