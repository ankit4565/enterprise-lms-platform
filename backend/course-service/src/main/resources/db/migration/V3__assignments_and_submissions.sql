-- ============================================================================
-- V3__assignments_and_submissions.sql
-- Enterprise LMS - Assignments and Submissions Schema
-- ============================================================================

-- 1. Assignments Table
CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NOT NULL,
    max_score INT NOT NULL DEFAULT 100,
    due_at TIMESTAMPTZ,
    allow_late BOOLEAN NOT NULL DEFAULT true,
    late_penalty_percent INT NOT NULL DEFAULT 0,
    allowed_file_types VARCHAR(255),
    max_file_size_mb INT NOT NULL DEFAULT 50,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assignments_lesson UNIQUE (lesson_id)
);

-- 2. Assignment Submissions Table
CREATE TABLE IF NOT EXISTS assignment_submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    attempt_no INT NOT NULL DEFAULT 1,
    text_answer TEXT,
    file_urls TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'GRADED', 'RETURNED')),
    is_late BOOLEAN NOT NULL DEFAULT false,
    raw_score NUMERIC(6,2),
    final_score NUMERIC(6,2),
    feedback TEXT,
    graded_by UUID,
    graded_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_assignment_student_attempt UNIQUE (assignment_id, student_id, attempt_no)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_assignments_course ON assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_assignments_lesson ON assignments(lesson_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment_status ON assignment_submissions(assignment_id, status);
CREATE INDEX IF NOT EXISTS idx_submissions_student_assignment ON assignment_submissions(student_id, assignment_id);
CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at ON assignment_submissions(submitted_at);
