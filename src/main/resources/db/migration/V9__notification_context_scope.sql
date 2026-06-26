ALTER TABLE user_notifications
    ADD COLUMN IF NOT EXISTS context VARCHAR(24);

UPDATE user_notifications
SET context = CASE
    WHEN type IN ('PROVIDER_VERIFICATION_DECISION', 'MATCHING_REQUEST_CREATED')
        THEN 'PROVIDER'
    WHEN type = 'CASE_MESSAGE_CREATED' AND link_type = 'CASE_REQUEST'
        THEN 'PROVIDER'
    ELSE 'CITIZEN'
END
WHERE context IS NULL;

ALTER TABLE user_notifications
    ALTER COLUMN context SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_context_created
    ON user_notifications(user_id, context, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_context_unread
    ON user_notifications(user_id, context, read_at);
