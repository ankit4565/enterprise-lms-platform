-- ============================================================================
-- V1__user_baseline.sql
-- Enterprise LMS - User Service Baseline Schema
-- ============================================================================

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS profiles (
    user_id UUID PRIMARY KEY,
    headline VARCHAR(160),
    bio TEXT,
    avatar_url VARCHAR(500),
    phone VARCHAR(30),
    country VARCHAR(100),
    timezone VARCHAR(50),
    language VARCHAR(10) DEFAULT 'en',
    website VARCHAR(255),
    linkedin VARCHAR(255),
    github VARCHAR(255),
    expertise_tags TEXT,
    is_public BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. User Preferences Table
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY,
    email_notifications BOOLEAN NOT NULL DEFAULT true,
    marketing_emails BOOLEAN NOT NULL DEFAULT false,
    course_updates BOOLEAN NOT NULL DEFAULT true,
    theme VARCHAR(20) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 3. Instructor Applications Table
CREATE TABLE IF NOT EXISTS instructor_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    qualifications TEXT NOT NULL,
    sample_url VARCHAR(500),
    reviewed_by UUID,
    review_note TEXT,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_instructor_apps_user ON instructor_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_instructor_apps_status ON instructor_applications(status);
CREATE INDEX IF NOT EXISTS idx_profiles_public ON profiles(is_public);
