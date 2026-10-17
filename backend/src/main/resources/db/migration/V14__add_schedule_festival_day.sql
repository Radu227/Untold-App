ALTER TABLE personal_schedule
    ADD COLUMN IF NOT EXISTS festival_day INT NOT NULL DEFAULT 1;
