ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS replaced_by_token_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'fk_refresh_tokens_replaced_by_token'
    ) THEN
        ALTER TABLE refresh_tokens
            ADD CONSTRAINT fk_refresh_tokens_replaced_by_token
            FOREIGN KEY (replaced_by_token_id)
            REFERENCES refresh_tokens(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_replaced_by_token_id
    ON refresh_tokens(replaced_by_token_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked_at
    ON refresh_tokens(revoked_at);
