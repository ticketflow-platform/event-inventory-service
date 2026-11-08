CREATE TABLE events.events_images (
    event_id UUID NOT NULL REFERENCES events.events (id),
    image_id UUID NOT NULL REFERENCES events.images (id),
    type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, image_id),
    CHECK (type IN ('COVER', 'GALLERY'))
);
