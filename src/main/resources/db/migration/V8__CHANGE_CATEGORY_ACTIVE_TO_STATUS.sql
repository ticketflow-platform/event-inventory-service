ALTER TABLE events.categories
    ADD COLUMN status VARCHAR(16);

UPDATE events.categories
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'INACTIVE' END;

ALTER TABLE events.categories
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    DROP COLUMN active;
