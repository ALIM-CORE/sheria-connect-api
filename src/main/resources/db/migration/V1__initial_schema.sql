CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE
);

CREATE TABLE authorities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roles_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, roles_id)
);

CREATE INDEX idx_users_roles_user_id ON users_roles(user_id);
CREATE INDEX idx_users_roles_roles_id ON users_roles(roles_id);

CREATE TABLE role_authorities (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    authority_id INTEGER NOT NULL REFERENCES authorities(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, authority_id)
);

CREATE INDEX idx_role_authorities_role_id ON role_authorities(role_id);
CREATE INDEX idx_role_authorities_authority_id ON role_authorities(authority_id);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_type VARCHAR(32),
    expiry_date TIMESTAMPTZ,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

CREATE TABLE email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMPTZ
);

CREATE INDEX idx_email_verification_token_user_id ON email_verification_token(user_id);

CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);

CREATE TABLE incident_reports (
    id BIGSERIAL PRIMARY KEY,
    case_number VARCHAR(32) NOT NULL UNIQUE,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    tracking_token_hash VARCHAR(128),
    anonymity_mode VARCHAR(32) NOT NULL,
    incident_type VARCHAR(64) NOT NULL,
    urgency VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(160),
    description TEXT NOT NULL,
    incident_date DATE,
    location_description TEXT,
    region VARCHAR(120),
    district VARCHAR(120),
    ward VARCHAR(120),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    pseudonym VARCHAR(120),
    contact_name VARCHAR(160),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(80),
    matching_requested BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_reports_case_number ON incident_reports(case_number);
CREATE INDEX idx_incident_reports_status ON incident_reports(status);
CREATE INDEX idx_incident_reports_reporter_user_id ON incident_reports(reporter_user_id);
CREATE INDEX idx_incident_reports_created_at ON incident_reports(created_at);

CREATE TABLE evidence_files (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    relative_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    upload_source VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_files_report_id ON evidence_files(incident_report_id);
CREATE INDEX idx_evidence_files_checksum ON evidence_files(checksum_sha256);

CREATE TABLE case_status_history (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_case_status_history_report_id ON case_status_history(incident_report_id);

CREATE TABLE admin_case_notes (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    admin_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_case_notes_report_id ON admin_case_notes(incident_report_id);

CREATE TABLE provider_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    provider_type VARCHAR(40) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    organization_name VARCHAR(180),
    email VARCHAR(180),
    phone VARCHAR(80),
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    availability_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    pricing_tier VARCHAR(32) NOT NULL DEFAULT 'FREE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_workload INTEGER NOT NULL DEFAULT 0,
    max_active_cases INTEGER NOT NULL DEFAULT 10,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_profiles_verification ON provider_profiles(verification_status);
CREATE INDEX idx_provider_profiles_availability ON provider_profiles(availability_status);
CREATE INDEX idx_provider_profiles_active ON provider_profiles(active);
CREATE INDEX idx_provider_profiles_user_id ON provider_profiles(user_id);

CREATE TABLE provider_profile_specialties (
    provider_profile_id BIGINT NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    incident_type VARCHAR(64) NOT NULL,
    PRIMARY KEY (provider_profile_id, incident_type)
);

CREATE INDEX idx_provider_profile_specialties_type ON provider_profile_specialties(incident_type);

CREATE TABLE provider_profile_regions (
    provider_profile_id BIGINT NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    region VARCHAR(120) NOT NULL,
    PRIMARY KEY (provider_profile_id, region)
);

CREATE INDEX idx_provider_profile_regions_region ON provider_profile_regions(region);

CREATE TABLE case_match_requests (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    provider_profile_id BIGINT NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    score INTEGER NOT NULL DEFAULT 0,
    score_breakdown TEXT,
    requested_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    decided_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_case_match_requests_report_provider UNIQUE (incident_report_id, provider_profile_id)
);

CREATE INDEX idx_case_match_requests_report_id ON case_match_requests(incident_report_id);
CREATE INDEX idx_case_match_requests_provider_id ON case_match_requests(provider_profile_id);
CREATE INDEX idx_case_match_requests_status ON case_match_requests(status);
