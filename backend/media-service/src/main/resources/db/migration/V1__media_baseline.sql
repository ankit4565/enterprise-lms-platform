-- Enterprise LMS Platform: Media Service Baseline Schema
-- Database: lms_media

CREATE TABLE IF NOT EXISTS media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    context_type VARCHAR(50) NOT NULL,
    context_id UUID,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    cdn_url VARCHAR(1000),
    hls_manifest_url VARCHAR(1000),
    duration_seconds INT DEFAULT 0,
    width INT,
    height INT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    checksum_sha256 VARCHAR(64),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_media_status CHECK (status IN ('PENDING', 'UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_media_context CHECK (context_type IN ('COURSE_VIDEO', 'PROMO', 'THUMBNAIL', 'ASSIGNMENT', 'CHAT_ATTACHMENT', 'AVATAR'))
);

CREATE INDEX IF NOT EXISTS idx_media_owner_id ON media(owner_id);
CREATE INDEX IF NOT EXISTS idx_media_context ON media(context_type, context_id);
CREATE INDEX IF NOT EXISTS idx_media_status ON media(status);
CREATE INDEX IF NOT EXISTS idx_media_is_deleted ON media(is_deleted);

CREATE TABLE IF NOT EXISTS upload_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    upload_id VARCHAR(255) NOT NULL,
    parts JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_upload_sessions_media_id ON upload_sessions(media_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_upload_id ON upload_sessions(upload_id);

CREATE TABLE IF NOT EXISTS transcode_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempt SMALLINT NOT NULL DEFAULT 1,
    renditions JSONB,
    error TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transcode_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_transcode_jobs_media_id ON transcode_jobs(media_id);
CREATE INDEX IF NOT EXISTS idx_transcode_jobs_status ON transcode_jobs(status);
