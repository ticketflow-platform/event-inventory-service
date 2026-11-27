DELETE FROM events.idempotency_keys;

DELETE FROM events.events_images;

DELETE FROM events.events_categories;

DELETE FROM events.events;

DELETE FROM events.images;

DELETE FROM events.categories;

INSERT INTO
  events.categories (id, name, status, created_at)
SELECT
  (
    '00000000-0000-0000-0000-' || LPAD(category_number::text, 12, '0')
  )::uuid,
  'Category ' || category_number,
  'ACTIVE',
  CURRENT_TIMESTAMP
FROM
  generate_series(1, 100) AS series (category_number);

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
  );
