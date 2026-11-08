CREATE TABLE events.images (
    id UUID PRIMARY KEY,
    organizer_id VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
