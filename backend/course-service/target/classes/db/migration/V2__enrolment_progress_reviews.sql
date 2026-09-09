-- ============================================================================
-- V2__enrolment_progress_reviews.sql
-- Enterprise LMS - Enrolments, Lesson Progress, and Course Reviews
-- ============================================================================

-- 1. Enrolments Table
CREATE TABLE IF NOT EXISTS enrolments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'FREE' CHECK (source IN ('FREE', 'PURCHASE', 'ADMIN_GRANT')),
    order_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'REFUNDED')),
    progress_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_enrolments_course_student UNIQUE (course_id, student_id)
);

-- 2. Lesson Progress Table
CREATE TABLE IF NOT EXISTS lesson_progress (
    id UUID PRIMARY KEY,
    enrolment_id UUID NOT NULL REFERENCES enrolments(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    last_position_seconds INT NOT NULL DEFAULT 0,
    watched_seconds INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_lp_enrolment_lesson UNIQUE (enrolment_id, lesson_id)
);

-- 3. Reviews Table
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    is_approved BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reviews_course_student UNIQUE (course_id, student_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_enrolments_student_status ON enrolments(student_id, status);
CREATE INDEX IF NOT EXISTS idx_enrolments_course ON enrolments(course_id);
CREATE INDEX IF NOT EXISTS idx_lp_enrolment ON lesson_progress(enrolment_id);
CREATE INDEX IF NOT EXISTS idx_lp_enrolment_status ON lesson_progress(enrolment_id, status);
CREATE INDEX IF NOT EXISTS idx_reviews_course ON reviews(course_id, is_approved);
CREATE INDEX IF NOT EXISTS idx_reviews_student ON reviews(student_id);
