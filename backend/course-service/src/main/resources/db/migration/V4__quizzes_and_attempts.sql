-- ============================================================================
-- V4__quizzes_and_attempts.sql
-- Enterprise LMS - Quizzes, Questions, Options, and Attempts Schema
-- ============================================================================

-- 1. Quizzes Table
CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    time_limit_minutes INT,
    max_attempts INT NOT NULL DEFAULT 1,
    pass_percent NUMERIC(5,2) NOT NULL DEFAULT 60.00,
    shuffle_questions BOOLEAN NOT NULL DEFAULT false,
    shuffle_options BOOLEAN NOT NULL DEFAULT false,
    show_answers_policy VARCHAR(30) NOT NULL DEFAULT 'AFTER_SUBMIT' CHECK (show_answers_policy IN ('NEVER', 'AFTER_SUBMIT', 'AFTER_DUE')),
    total_marks NUMERIC(6,2) NOT NULL DEFAULT 0.00,
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_quizzes_lesson UNIQUE (lesson_id)
);

-- 2. Quiz Questions Table
CREATE TABLE IF NOT EXISTS quiz_questions (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'NUMERIC')),
    text TEXT NOT NULL,
    marks NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    negative_marks NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    explanation TEXT,
    position INT NOT NULL DEFAULT 0,
    correct_text TEXT,
    numeric_answer NUMERIC(10,4),
    tolerance NUMERIC(8,4) DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 3. Quiz Options Table
CREATE TABLE IF NOT EXISTS quiz_options (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    text VARCHAR(500) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. Quiz Attempts Table
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id UUID PRIMARY KEY,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    attempt_no INT NOT NULL DEFAULT 1,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'EXPIRED')),
    score NUMERIC(6,2),
    percentage NUMERIC(5,2),
    passed BOOLEAN,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    unanswered_count INT DEFAULT 0,
    time_taken_seconds INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_quiz_student_attempt UNIQUE (quiz_id, student_id, attempt_no)
);

-- 5. Attempt Answers Table
CREATE TABLE IF NOT EXISTS attempt_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    selected_option_ids TEXT,
    text_answer TEXT,
    numeric_answer NUMERIC(10,4),
    is_correct BOOLEAN,
    marks_awarded NUMERIC(6,2) DEFAULT 0.00,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_quizzes_course ON quizzes(course_id);
CREATE INDEX IF NOT EXISTS idx_quizzes_lesson ON quizzes(lesson_id);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_quiz ON quiz_questions(quiz_id);
CREATE INDEX IF NOT EXISTS idx_quiz_options_question ON quiz_options(question_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_quiz_student ON quiz_attempts(quiz_id, student_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_student ON quiz_attempts(student_id);
CREATE INDEX IF NOT EXISTS idx_attempt_answers_attempt ON attempt_answers(attempt_id);
