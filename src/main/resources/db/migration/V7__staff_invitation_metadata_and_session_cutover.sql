ALTER TABLE user_role_assignments
    ADD COLUMN staff_invitation_id BIGINT REFERENCES staff_invitations(id) ON DELETE SET NULL;

CREATE INDEX idx_role_assignment_invitation
    ON user_role_assignments(staff_invitation_id);

ALTER TABLE staff_invitations
    ADD COLUMN job_title VARCHAR(160),
    ADD COLUMN department VARCHAR(160),
    ADD COLUMN employee_number VARCHAR(100),
    ADD COLUMN work_email VARCHAR(255),
    ADD COLUMN grant_reason TEXT,
    ADD COLUMN access_expires_at TIMESTAMPTZ;

-- Legacy refresh tokens do not carry a context or server-side session.
DELETE FROM refresh_tokens WHERE auth_session_id IS NULL;
