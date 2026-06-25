ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(32) NOT NULL DEFAULT 'CITIZEN',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS suspension_reason TEXT,
    ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS audience VARCHAR(32) NOT NULL DEFAULT 'PLATFORM_STAFF',
    ADD COLUMN IF NOT EXISTS system_role BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS editable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deletable BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE roles
SET display_name = INITCAP(REPLACE(LOWER(name), '_', ' '))
WHERE display_name IS NULL OR BTRIM(display_name) = '';

ALTER TABLE roles ALTER COLUMN display_name SET NOT NULL;

UPDATE roles
SET audience = 'PRODUCT_IDENTITY',
    system_role = TRUE,
    editable = FALSE,
    deletable = FALSE
WHERE name IN ('CITIZEN', 'PROVIDER');

UPDATE roles
SET audience = 'PLATFORM_STAFF',
    system_role = TRUE,
    deletable = FALSE
WHERE name IN ('SUPER_ADMIN', 'SYSTEM_ADMIN');

UPDATE roles SET editable = FALSE WHERE name = 'SUPER_ADMIN';

UPDATE users u
SET account_type = CASE
    WHEN EXISTS (
        SELECT 1
        FROM users_roles ur
        JOIN roles r ON r.id = ur.roles_id
        WHERE ur.user_id = u.id AND r.name IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
    ) THEN 'PLATFORM_STAFF'
    WHEN EXISTS (
        SELECT 1
        FROM users_roles ur
        JOIN roles r ON r.id = ur.roles_id
        WHERE ur.user_id = u.id AND r.name = 'PROVIDER'
    ) THEN 'PROVIDER'
    ELSE 'CITIZEN'
END;

CREATE INDEX IF NOT EXISTS idx_users_account_type ON users(account_type);
CREATE INDEX IF NOT EXISTS idx_users_active_locked ON users(active, locked);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_roles_audience ON roles(audience);

CREATE TABLE IF NOT EXISTS staff_invitations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    invited_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    accepted_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS staff_invitation_roles (
    invitation_id BIGINT NOT NULL REFERENCES staff_invitations(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (invitation_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_invitations_email ON staff_invitations(email);
CREATE INDEX IF NOT EXISTS idx_staff_invitations_status ON staff_invitations(status);
CREATE INDEX IF NOT EXISTS idx_staff_invitations_expires_at ON staff_invitations(expires_at);

CREATE TABLE IF NOT EXISTS access_audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    target_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_access_audit_actor ON access_audit_events(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_access_audit_target ON access_audit_events(target_user_id);
CREATE INDEX IF NOT EXISTS idx_access_audit_created_at ON access_audit_events(created_at);
