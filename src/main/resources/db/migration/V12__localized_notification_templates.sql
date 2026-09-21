ALTER TABLE user_notifications
    ADD COLUMN title_key VARCHAR(120),
    ADD COLUMN body_key VARCHAR(120),
    ADD COLUMN params JSONB;
