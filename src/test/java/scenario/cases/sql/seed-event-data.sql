DELETE FROM events.idempotency_keys;

DELETE FROM events.events_images;

DELETE FROM events.events_categories;

DELETE FROM events.events;

DELETE FROM events.images;

DELETE FROM events.categories;

INSERT INTO
  events.categories (id, name, status, created_at)
VALUES
  (
    '00000000-0000-0000-0000-000000000011',
    'Music',
    'ACTIVE',
    CURRENT_TIMESTAMP
  );

INSERT INTO
  events.categories (id, name, status, created_at)
VALUES
  (
    '00000000-0000-0000-0000-000000000013',
    'Inactive',
    'INACTIVE',
    CURRENT_TIMESTAMP
  );

INSERT INTO
  events.images (
    id,
    organizer_id,
    storage_key,
    content_type,
    file_size,
    created_at
  )
VALUES
  (
    '00000000-0000-0000-0000-000000000012',
    'organizer-1',
    'events/cover.jpg',
    'image/jpeg',
    1024,
    CURRENT_TIMESTAMP
  ),
  (
    '00000000-0000-0000-0000-000000000014',
    'organizer-1',
    'events/gallery.jpg',
    'image/jpeg',
    2048,
    CURRENT_TIMESTAMP
  ),
  (
    '00000000-0000-0000-0000-000000000015',
    'organizer-2',
    'events/foreign.jpg',
    'image/jpeg',
    3072,
    CURRENT_TIMESTAMP
  );
