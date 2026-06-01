ALTER TABLE provider_profiles
    ADD COLUMN IF NOT EXISTS license_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS verification_rejection_reason TEXT;

CREATE TABLE IF NOT EXISTS provider_profile_languages (
    provider_profile_id BIGINT NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    language VARCHAR(80) NOT NULL,
    PRIMARY KEY (provider_profile_id, language)
);

CREATE INDEX IF NOT EXISTS idx_provider_profile_languages_language
    ON provider_profile_languages(language);

CREATE TABLE IF NOT EXISTS case_messages (
    id BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    case_match_request_id BIGINT REFERENCES case_match_requests(id) ON DELETE SET NULL,
    sender_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    sender_role VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_case_messages_report_id
    ON case_messages(incident_report_id);
CREATE INDEX IF NOT EXISTS idx_case_messages_match_request_id
    ON case_messages(case_match_request_id);
CREATE INDEX IF NOT EXISTS idx_case_messages_created_at
    ON case_messages(created_at);

CREATE TABLE IF NOT EXISTS user_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(180) NOT NULL,
    body TEXT,
    link_type VARCHAR(64),
    link_target VARCHAR(180),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_id
    ON user_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_read_at
    ON user_notifications(read_at);
CREATE INDEX IF NOT EXISTS idx_user_notifications_created_at
    ON user_notifications(created_at);

CREATE TABLE IF NOT EXISTS legal_knowledge_articles (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(220) NOT NULL,
    category VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_legal_knowledge_articles_category
    ON legal_knowledge_articles(category);
CREATE INDEX IF NOT EXISTS idx_legal_knowledge_articles_published
    ON legal_knowledge_articles(published);
CREATE INDEX IF NOT EXISTS idx_legal_knowledge_articles_sort
    ON legal_knowledge_articles(sort_order, title);

INSERT INTO legal_knowledge_articles
    (slug, title, category, summary, body, published, sort_order)
VALUES
    (
        'what-to-do-after-rights-violation',
        'What to do after a rights violation',
        'Getting help',
        'Practical first steps for documenting what happened and getting safe support.',
        'If you believe your rights have been violated, first move to a safe place if you can. Write down what happened while the details are fresh, including dates, locations, names of institutions, and any witnesses. Keep photos, documents, messages, or medical records that may support your account. Do not share sensitive details publicly if doing so may put you or another person at risk. Use Sheria Connect to create a private report when you are ready, and choose the anonymity level that matches your safety needs.',
        TRUE,
        10
    ),
    (
        'understanding-anonymous-reporting',
        'Understanding anonymous reporting',
        'Safety',
        'How anonymous reports work and what you should keep private.',
        'Anonymous reporting helps you document an incident without attaching your public identity to the report. You should still avoid adding unnecessary identifying details if you are at risk. After submitting an anonymous report, keep the case number and tracking token somewhere safe because they are needed to open the report again. Anonymous reporting is different from public Stories: private reports are not published.',
        TRUE,
        20
    ),
    (
        'preparing-evidence-safely',
        'Preparing evidence safely',
        'Evidence',
        'Guidance for collecting photos, documents, audio, or video without increasing risk.',
        'Useful evidence can include photos, documents, audio, video, receipts, official letters, messages, or medical notes. Only collect evidence when it is safe to do so. Do not confront someone or enter a dangerous place to collect evidence. Before uploading public-facing content, remove names, phone numbers, addresses, and case numbers. For private reports, upload only files that help explain what happened.',
        TRUE,
        30
    ),
    (
        'how-provider-matching-works',
        'How provider matching works',
        'Legal support',
        'What happens when you request advocate or NGO matching.',
        'When you request matching, Sheria Connect owners review your report and look for verified legal providers or organizations whose specialties, regions, availability, and workload fit the case. A provider may accept or decline a request. If a provider accepts, the case can move into an active support workspace. Matching is handled carefully so sensitive information is not sent widely.',
        TRUE,
        40
    )
ON CONFLICT (slug) DO NOTHING;
