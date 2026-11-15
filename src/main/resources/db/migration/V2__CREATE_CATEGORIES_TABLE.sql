CREATE TABLE events.categories
(
    id         UUID PRIMARY KEY,
    status     VARCHAR(64)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);
