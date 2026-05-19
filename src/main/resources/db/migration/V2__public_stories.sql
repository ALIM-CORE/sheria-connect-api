CREATE TABLE IF NOT EXISTS public_stories (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    author_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(80) NOT NULL,
    region VARCHAR(120),
    district VARCHAR(120),
    anonymity_mode VARCHAR(32) NOT NULL,
    display_name VARCHAR(120),
    moderation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_public_stories_public_id ON public_stories(public_id);
CREATE INDEX IF NOT EXISTS idx_public_stories_status ON public_stories(moderation_status);
CREATE INDEX IF NOT EXISTS idx_public_stories_category ON public_stories(category);
CREATE INDEX IF NOT EXISTS idx_public_stories_region ON public_stories(region);
CREATE INDEX IF NOT EXISTS idx_public_stories_created_at ON public_stories(created_at);

CREATE TABLE IF NOT EXISTS story_reactions (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES public_stories(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction_type VARCHAR(32) NOT NULL DEFAULT 'SOLIDARITY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_story_reactions_story_user UNIQUE (story_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_story_reactions_story_id ON story_reactions(story_id);
CREATE INDEX IF NOT EXISTS idx_story_reactions_user_id ON story_reactions(user_id);

CREATE TABLE IF NOT EXISTS story_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES public_stories(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_story_bookmarks_story_user UNIQUE (story_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_story_bookmarks_story_id ON story_bookmarks(story_id);
CREATE INDEX IF NOT EXISTS idx_story_bookmarks_user_id ON story_bookmarks(user_id);

CREATE TABLE IF NOT EXISTS story_content_reports (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES public_stories(id) ON DELETE CASCADE,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(80) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_story_content_reports_story_id ON story_content_reports(story_id);
CREATE INDEX IF NOT EXISTS idx_story_content_reports_reporter_user_id ON story_content_reports(reporter_user_id);

CREATE TABLE IF NOT EXISTS story_moderation_notes (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES public_stories(id) ON DELETE CASCADE,
    admin_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_story_moderation_notes_story_id ON story_moderation_notes(story_id);
CREATE INDEX IF NOT EXISTS idx_story_moderation_notes_admin_user_id ON story_moderation_notes(admin_user_id);
