ALTER TABLE events.events
    ALTER COLUMN age_restriction DROP NOT NULL,
    ALTER COLUMN age_restriction DROP DEFAULT;
