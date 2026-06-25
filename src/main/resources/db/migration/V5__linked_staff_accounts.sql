ALTER TABLE staff_invitations
    ADD COLUMN IF NOT EXISTS linked_product_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_staff_invitations_linked_product_user
    ON staff_invitations(linked_product_user_id);

CREATE TABLE IF NOT EXISTS user_account_links (
    id BIGSERIAL PRIMARY KEY,
    staff_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    product_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    linked_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_account_links_product UNIQUE (product_user_id),
    CONSTRAINT uk_user_account_links_pair UNIQUE (staff_user_id, product_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_account_links_staff
    ON user_account_links(staff_user_id);

CREATE INDEX IF NOT EXISTS idx_user_account_links_created_at
    ON user_account_links(created_at);
