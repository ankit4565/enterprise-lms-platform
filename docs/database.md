# Enterprise LMS Platform — Database Documentation

## 1. Auth Service Database (`lms_auth`)

### Tables
- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`
- `refresh_tokens`
- `otps`
- `oauth_accounts`
- `audit_logs`

### Purpose
The Auth Service is responsible for identity, credential authentication, session tokens, and Role-Based Access Control (RBAC). It issues HMAC-signed JWT access tokens and family-tracked refresh tokens with reuse compromise detection.

---

## 2. User Service Database (`lms_user`)

### Tables
- `profiles`
  - `user_id` (UUID PK)
  - `headline` (VARCHAR 160)
  - `bio` (TEXT)
  - `avatar_url` (VARCHAR 500)
  - `phone`, `country`, `timezone`, `language`
  - `website`, `linkedin`, `github`
  - `expertise_tags` (TEXT)
  - `is_public` (BOOLEAN DEFAULT true)
  - `created_at`, `updated_at`, `version`
- `user_preferences`
  - `user_id` (UUID PK)
  - `email_notifications` (BOOLEAN DEFAULT true)
  - `marketing_emails` (BOOLEAN DEFAULT false)
  - `course_updates` (BOOLEAN DEFAULT true)
  - `theme` (VARCHAR 20 DEFAULT 'system')
  - `created_at`, `updated_at`, `version`
- `instructor_applications`
  - `id` (UUID PK)
  - `user_id` (UUID NOT NULL)
  - `status` (VARCHAR 20 CHECK IN ('PENDING', 'APPROVED', 'REJECTED'))
  - `qualifications` (TEXT NOT NULL)
  - `sample_url` (VARCHAR 500)
  - `reviewed_by` (UUID)
  - `review_note` (TEXT)
  - `reviewed_at` (TIMESTAMPTZ)
  - `created_at`, `updated_at`, `version`

### Purpose
The User Service manages user public and private profiles, avatar hooks, communication preferences, and the instructor onboarding application lifecycle.

---

## 3. Course Service Database (`lms_course`)

### Tables
- `categories`
  - `id` (UUID PK)
  - `name` (VARCHAR 100 NOT NULL)
  - `slug` (VARCHAR 120 UNIQUE NOT NULL)
  - `parent_id` (UUID FK -> categories.id ON DELETE SET NULL)
  - `icon` (VARCHAR 100)
  - `display_order` (INT NOT NULL DEFAULT 0)
  - `is_active` (BOOLEAN NOT NULL DEFAULT true)
  - `created_at`, `updated_at`, `version`
- `courses`
  - `id` (UUID PK)
  - `instructor_id` (UUID NOT NULL) — logical reference to `users.id`
  - `category_id` (UUID FK -> categories.id NOT NULL)
  - `title` (VARCHAR 160 NOT NULL)
  - `slug` (VARCHAR 180 UNIQUE NOT NULL)
  - `subtitle` (VARCHAR 255)
  - `description` (TEXT NOT NULL)
  - `level` (VARCHAR 20 CHECK IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL'))
  - `language` (VARCHAR 10 DEFAULT 'en')
  - `thumbnail_url` (VARCHAR 500)
  - `promo_video_id` (UUID)
  - `price_minor` (BIGINT NOT NULL DEFAULT 0)
  - `currency` (CHAR 3 DEFAULT 'INR')
  - `discount_price_minor` (BIGINT)
  - `status` (VARCHAR 20 CHECK IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED'))
  - `rating_avg` (NUMERIC 3,2 DEFAULT 0)
  - `rating_count` (INT DEFAULT 0)
  - `enrolment_count` (INT DEFAULT 0)
  - `total_duration_seconds` (INT DEFAULT 0)
  - `tags` (TEXT)
  - `requirements` (TEXT)
  - `outcomes` (TEXT)
  - `published_at` (TIMESTAMPTZ)
  - `rejected_reason` (TEXT)
  - `created_at`, `updated_at`, `deleted_at`, `version`
- `modules`
  - `id` (UUID PK)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `title` (VARCHAR 200 NOT NULL)
  - `description` (TEXT)
  - `position` (INT NOT NULL)
  - `created_at`, `updated_at`, `version`
- `lessons`
  - `id` (UUID PK)
  - `module_id` (UUID FK -> modules.id ON DELETE CASCADE NOT NULL)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `title` (VARCHAR 200 NOT NULL)
  - `type` (VARCHAR 20 CHECK IN ('VIDEO', 'ARTICLE', 'QUIZ', 'ASSIGNMENT', 'RESOURCE'))
  - `content` (TEXT)
  - `media_id` (UUID)
  - `duration_seconds` (INT DEFAULT 0)
  - `position` (INT NOT NULL)
  - `is_free_preview` (BOOLEAN DEFAULT false)
  - `is_published` (BOOLEAN DEFAULT true)
  - `created_at`, `updated_at`, `version`
- `enrolments`
  - `id` (UUID PK)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `student_id` (UUID NOT NULL)
  - `source` (VARCHAR 20 CHECK IN ('FREE', 'PURCHASE', 'ADMIN_GRANT'))
  - `order_id` (UUID)
  - `status` (VARCHAR 20 CHECK IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'REFUNDED'))
  - `progress_percent` (NUMERIC 5,2 DEFAULT 0)
  - `enrolled_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - `completed_at` (TIMESTAMPTZ)
  - `last_accessed_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (course_id, student_id)`
- `lesson_progress`
  - `id` (UUID PK)
  - `enrolment_id` (UUID FK -> enrolments.id ON DELETE CASCADE NOT NULL)
  - `lesson_id` (UUID FK -> lessons.id ON DELETE CASCADE NOT NULL)
  - `status` (VARCHAR 20 CHECK IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))
  - `last_position_seconds` (INT DEFAULT 0)
  - `watched_seconds` (INT DEFAULT 0)
  - `completed_at` (TIMESTAMPTZ)
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (enrolment_id, lesson_id)`
- `reviews`
  - `id` (UUID PK)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `student_id` (UUID NOT NULL)
  - `rating` (SMALLINT CHECK BETWEEN 1 AND 5)
  - `comment` (TEXT)
  - `is_approved` (BOOLEAN DEFAULT true)
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (course_id, student_id)`
- `assignments`
  - `id` (UUID PK)
  - `lesson_id` (UUID FK -> lessons.id ON DELETE CASCADE NOT NULL UNIQUE)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `title` (VARCHAR 200 NOT NULL)
  - `instructions` (TEXT NOT NULL)
  - `max_score` (INT DEFAULT 100)
  - `due_at` (TIMESTAMPTZ)
  - `allow_late` (BOOLEAN DEFAULT true)
  - `late_penalty_percent` (INT DEFAULT 0)
  - `allowed_file_types` (VARCHAR 255)
  - `max_file_size_mb` (INT DEFAULT 50)
  - `created_at`, `updated_at`, `version`
- `assignment_submissions`
  - `id` (UUID PK)
  - `assignment_id` (UUID FK -> assignments.id ON DELETE CASCADE NOT NULL)
  - `student_id` (UUID NOT NULL)
  - `attempt_no` (INT DEFAULT 1)
  - `text_answer` (TEXT)
  - `file_urls` (TEXT)
  - `status` (VARCHAR 30 CHECK IN ('SUBMITTED', 'UNDER_REVIEW', 'GRADED', 'RETURNED'))
  - `is_late` (BOOLEAN DEFAULT false)
  - `raw_score` (NUMERIC 6,2)
  - `final_score` (NUMERIC 6,2)
  - `feedback` (TEXT)
  - `graded_by` (UUID)
  - `graded_at` (TIMESTAMPTZ)
  - `submitted_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (assignment_id, student_id, attempt_no)`
- `quizzes`
  - `id` (UUID PK)
  - `lesson_id` (UUID FK -> lessons.id ON DELETE CASCADE NOT NULL UNIQUE)
  - `course_id` (UUID FK -> courses.id ON DELETE CASCADE NOT NULL)
  - `title` (VARCHAR 200 NOT NULL)
  - `description` (TEXT)
  - `time_limit_minutes` (INT)
  - `max_attempts` (INT DEFAULT 1)
  - `pass_percent` (NUMERIC 5,2 DEFAULT 60.00)
  - `shuffle_questions` (BOOLEAN DEFAULT false)
  - `shuffle_options` (BOOLEAN DEFAULT false)
  - `show_answers_policy` (VARCHAR 30 CHECK IN ('NEVER', 'AFTER_SUBMIT', 'AFTER_DUE') DEFAULT 'AFTER_SUBMIT')
  - `total_marks` (NUMERIC 6,2 DEFAULT 0.00)
  - `is_published` (BOOLEAN DEFAULT true)
  - `created_at`, `updated_at`, `version`
- `quiz_questions`
  - `id` (UUID PK)
  - `quiz_id` (UUID FK -> quizzes.id ON DELETE CASCADE NOT NULL)
  - `type` (VARCHAR 30 CHECK IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'NUMERIC'))
  - `text` (TEXT NOT NULL)
  - `marks` (NUMERIC 5,2 DEFAULT 1.00)
  - `negative_marks` (NUMERIC 5,2 DEFAULT 0.00)
  - `explanation` (TEXT)
  - `position` (INT NOT NULL DEFAULT 0)
  - `correct_text` (TEXT)
  - `numeric_answer` (NUMERIC 10,4)
  - `tolerance` (NUMERIC 8,4 DEFAULT 0.0000)
  - `created_at`, `updated_at`, `version`
- `quiz_options`
  - `id` (UUID PK)
  - `question_id` (UUID FK -> quiz_questions.id ON DELETE CASCADE NOT NULL)
  - `text` (VARCHAR 500 NOT NULL)
  - `is_correct` (BOOLEAN DEFAULT false)
  - `position` (INT NOT NULL DEFAULT 0)
  - `created_at`, `updated_at`, `version`
- `quiz_attempts`
  - `id` (UUID PK)
  - `quiz_id` (UUID FK -> quizzes.id ON DELETE CASCADE NOT NULL)
  - `student_id` (UUID NOT NULL)
  - `attempt_no` (INT DEFAULT 1)
  - `started_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - `expires_at` (TIMESTAMPTZ)
  - `submitted_at` (TIMESTAMPTZ)
  - `status` (VARCHAR 30 CHECK IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'EXPIRED') DEFAULT 'IN_PROGRESS')
  - `score` (NUMERIC 6,2)
  - `percentage` (NUMERIC 5,2)
  - `passed` (BOOLEAN)
  - `correct_count` (INT DEFAULT 0)
  - `wrong_count` (INT DEFAULT 0)
  - `unanswered_count` (INT DEFAULT 0)
  - `time_taken_seconds` (INT)
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (quiz_id, student_id, attempt_no)`
- `attempt_answers`
  - `id` (UUID PK)
  - `attempt_id` (UUID FK -> quiz_attempts.id ON DELETE CASCADE NOT NULL)
  - `question_id` (UUID FK -> quiz_questions.id ON DELETE CASCADE NOT NULL)
  - `selected_option_ids` (TEXT)
  - `text_answer` (TEXT)
  - `numeric_answer` (NUMERIC 10,4)
  - `is_correct` (BOOLEAN)
  - `marks_awarded` (NUMERIC 6,2 DEFAULT 0.00)
  - `answered_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - `created_at`, `updated_at`, `version`
  - `UNIQUE (attempt_id, question_id)`

### Purpose
The Course Service manages the core learning curriculum: categories, courses, position-ordered modules and lessons, student enrolments, lesson progress tracking (with video auto-completion at 90%), course completion calculation, course ratings & reviews, assignment authoring and grading, and quizzes engine (time-limited attempts, autosaving, negative marking, question/option shuffling, and auto-evaluation across single-choice, multi-choice, true/false, short answer, and numeric questions).

---

## 4. Media Service Database (`lms_media`)

### Tables
- `media`
  - `id` (UUID PK DEFAULT gen_random_uuid())
  - `owner_id` (UUID NOT NULL) — logical reference to `users.id`
  - `context_type` (VARCHAR 50 CHECK IN ('COURSE_VIDEO', 'PROMO', 'THUMBNAIL', 'ASSIGNMENT', 'CHAT_ATTACHMENT', 'AVATAR'))
  - `context_id` (UUID) — logical reference to associated entity (e.g., course_id, lesson_id, assignment_id)
  - `original_filename` (VARCHAR 255 NOT NULL)
  - `mime_type` (VARCHAR 100 NOT NULL)
  - `size_bytes` (BIGINT NOT NULL)
  - `storage_key` (VARCHAR 500 NOT NULL)
  - `cdn_url` (VARCHAR 1000)
  - `hls_manifest_url` (VARCHAR 1000)
  - `duration_seconds` (INT DEFAULT 0)
  - `width` (INT)
  - `height` (INT)
  - `status` (VARCHAR 50 CHECK IN ('PENDING', 'UPLOADED', 'PROCESSING', 'READY', 'FAILED'))
  - `checksum_sha256` (VARCHAR 64)
  - `is_deleted` (BOOLEAN NOT NULL DEFAULT false)
  - `deleted_at` (TIMESTAMPTZ)
  - `created_at`, `updated_at`, `version`
- `upload_sessions`
  - `id` (UUID PK DEFAULT gen_random_uuid())
  - `media_id` (UUID FK -> media.id ON DELETE CASCADE NOT NULL)
  - `upload_id` (VARCHAR 255 NOT NULL) — S3 / MinIO multipart upload ID
  - `parts` (JSONB) — part numbers, upload URLs, and part sizes
  - `expires_at` (TIMESTAMPTZ NOT NULL)
  - `completed_at` (TIMESTAMPTZ)
  - `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
- `transcode_jobs`
  - `id` (UUID PK DEFAULT gen_random_uuid())
  - `media_id` (UUID FK -> media.id ON DELETE CASCADE NOT NULL)
  - `status` (VARCHAR 50 CHECK IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
  - `attempt` (SMALLINT NOT NULL DEFAULT 1)
  - `renditions` (JSONB) — array of generated HLS renditions (1080p, 720p, 480p, 360p) with bitrates and resolutions
  - `error` (TEXT)
  - `started_at` (TIMESTAMPTZ)
  - `finished_at` (TIMESTAMPTZ)
  - `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())

### Purpose
The Media Service manages all object storage uploads and media lifecycle operations using direct-to-S3 presigned multipart upload sessions (so media binaries bypass application servers). It validates MIME types and strict size limits, triggers an asynchronous transcode workflow upon upload completion to generate multi-bitrate HLS playlists (`master.m3u8`) and thumbnail metadata with exponential backoff retries, and generates gated signed playback stream URLs valid for 30 minutes.