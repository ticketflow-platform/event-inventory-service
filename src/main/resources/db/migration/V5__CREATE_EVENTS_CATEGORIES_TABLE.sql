CREATE TABLE events.events_categories (
    event_id UUID NOT NULL REFERENCES events.events (id),
    category_id UUID NOT NULL REFERENCES events.categories (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, category_id)
);
