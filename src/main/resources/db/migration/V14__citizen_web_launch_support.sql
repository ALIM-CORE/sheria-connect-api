ALTER TABLE incident_reports
    ADD COLUMN submission_id UUID,
    ADD COLUMN submission_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX uk_incident_reports_submission_id
    ON incident_reports(submission_id)
    WHERE submission_id IS NOT NULL;

CREATE TABLE incident_report_replies (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL
        REFERENCES incident_reports(id) ON DELETE CASCADE,
    needs_info_history_id BIGINT NOT NULL
        REFERENCES case_status_history(id) ON DELETE CASCADE,
    submitted_by_user_id BIGINT
        REFERENCES users(id) ON DELETE SET NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_incident_report_replies_request UNIQUE (needs_info_history_id),
    CONSTRAINT chk_incident_report_replies_body_length
        CHECK (char_length(body) BETWEEN 1 AND 4000)
);

CREATE INDEX idx_incident_report_replies_report_id
    ON incident_report_replies(incident_report_id);

CREATE INDEX idx_incident_report_replies_created_at
    ON incident_report_replies(created_at);
