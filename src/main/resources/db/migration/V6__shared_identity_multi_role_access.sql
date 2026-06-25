DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM user_account_links LIMIT 1)
       OR EXISTS (
            SELECT 1 FROM staff_invitations
            WHERE linked_product_user_id IS NOT NULL
              AND status = 'ACCEPTED'
       ) THEN
        RAISE EXCEPTION 'V6 requires manual reconciliation because linked duplicate staff accounts exist';
    END IF;
END $$;

CREATE TABLE user_role_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    context VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    granted_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    changed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activated_at TIMESTAMPTZ,
    suspended_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_role_assignment_user_context
    ON user_role_assignments(user_id, context, status);
CREATE INDEX idx_role_assignment_expiry
    ON user_role_assignments(expires_at);
CREATE UNIQUE INDEX uk_active_user_role_context
    ON user_role_assignments(user_id, role_id, context)
    WHERE status IN ('PENDING', 'ACTIVE', 'SUSPENDED');

CREATE TABLE staff_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    employment_status VARCHAR(24) NOT NULL,
    job_title VARCHAR(160),
    department VARCHAR(160),
    employee_number VARCHAR(100),
    work_email VARCHAR(255),
    grant_reason TEXT NOT NULL,
    granted_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    activated_at TIMESTAMPTZ,
    suspended_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_staff_profiles_employee_number
    ON staff_profiles(employee_number)
    WHERE employee_number IS NOT NULL;

CREATE TABLE auth_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    active_context VARCHAR(24) NOT NULL,
    client_type VARCHAR(16) NOT NULL,
    audience VARCHAR(40) NOT NULL,
    mfa_verified BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_sessions_user_context
    ON auth_sessions(user_id, active_context, revoked);

ALTER TABLE refresh_tokens
    ADD COLUMN auth_session_id BIGINT REFERENCES auth_sessions(id) ON DELETE CASCADE;
CREATE INDEX idx_refresh_tokens_session ON refresh_tokens(auth_session_id);

CREATE TABLE staff_mfa_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    encrypted_secret TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mfa_recovery_codes (
    id BIGSERIAL PRIMARY KEY,
    credential_id BIGINT NOT NULL REFERENCES staff_mfa_credentials(id) ON DELETE CASCADE,
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auth_challenges (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_invitation_id BIGINT REFERENCES staff_invitations(id) ON DELETE CASCADE,
    purpose VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_challenges_expiry ON auth_challenges(expires_at);

ALTER TABLE access_audit_events
    ADD COLUMN session_id VARCHAR(36),
    ADD COLUMN ip_address VARCHAR(64),
    ADD COLUMN reason TEXT,
    ADD COLUMN before_value TEXT,
    ADD COLUMN after_value TEXT,
    ADD COLUMN result VARCHAR(24),
    ADD COLUMN correlation_id VARCHAR(64);

CREATE UNIQUE INDEX uk_provider_profiles_user
    ON provider_profiles(user_id)
    WHERE user_id IS NOT NULL;

INSERT INTO user_role_assignments (
    user_id,
    role_id,
    context,
    status,
    granted_at,
    activated_at,
    reason
)
SELECT
    ur.user_id,
    ur.roles_id,
    CASE
        WHEN r.audience = 'PLATFORM_STAFF' THEN 'STAFF'
        WHEN r.name = 'PROVIDER' THEN 'PROVIDER'
        ELSE 'CITIZEN'
    END,
    'ACTIVE',
    NOW(),
    NOW(),
    'Backfilled from legacy users_roles by V6'
FROM users_roles ur
JOIN roles r ON r.id = ur.roles_id
ON CONFLICT DO NOTHING;

INSERT INTO staff_profiles (
    user_id,
    employment_status,
    job_title,
    department,
    grant_reason,
    activated_at
)
SELECT DISTINCT
    ura.user_id,
    'ACTIVE',
    'Platform Staff',
    'Platform Operations',
    'Backfilled from existing staff access by V6',
    NOW()
FROM user_role_assignments ura
WHERE ura.context = 'STAFF'
  AND ura.status = 'ACTIVE'
ON CONFLICT (user_id) DO NOTHING;
