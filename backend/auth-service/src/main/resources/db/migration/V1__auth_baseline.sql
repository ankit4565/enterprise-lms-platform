-- Enable pgcrypto extension for UUIDs and hashing
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DELETED')),
    email_verified_at TIMESTAMPTZ,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(64),
    failed_login_count SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_users_email_active ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 2. roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(40) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 3. permissions table
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(60) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. user_roles join table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- 5. role_permissions join table
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 6. refresh_tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) UNIQUE NOT NULL,
    family_id UUID NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rt_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_rt_family ON refresh_tokens (family_id);

-- 7. otps table
CREATE TABLE otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'LOGIN_2FA', 'PASSWORD_RESET', 'EMAIL_CHANGE')),
    code_hash VARCHAR(128) NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    max_attempts SMALLINT NOT NULL DEFAULT 5,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_email_purpose ON otps (email, purpose);

-- 8. oauth_accounts table
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uc_oauth_provider_uid UNIQUE (provider, provider_user_id)
);

-- 9. audit_logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    actor_role VARCHAR(40),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    before JSONB,
    after JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    correlation_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_actor_created ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);

-- 10. Seed Data: Roles
INSERT INTO roles (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'STUDENT', 'Enrolled learner accessing courses and assignments'),
    ('00000000-0000-0000-0000-000000000002', 'INSTRUCTOR', 'Course author and instructor with grading permissions'),
    ('00000000-0000-0000-0000-000000000003', 'ADMIN', 'Platform administrator managing users and catalogue'),
    ('00000000-0000-0000-0000-000000000004', 'SUPER_ADMIN', 'Platform owner with full privileges');

-- Seed Data: Permissions
INSERT INTO permissions (id, code, description) VALUES
    ('00000000-0000-0000-0001-000000000001', 'COURSE_VIEW', 'Browse and view published courses'),
    ('00000000-0000-0000-0001-000000000002', 'COURSE_ENROL', 'Enrol in courses'),
    ('00000000-0000-0000-0001-000000000003', 'COURSE_CREATE', 'Create draft courses'),
    ('00000000-0000-0000-0001-000000000004', 'COURSE_UPDATE', 'Update owned courses'),
    ('00000000-0000-0000-0001-000000000005', 'COURSE_SUBMIT', 'Submit courses for review'),
    ('00000000-0000-0000-0001-000000000006', 'COURSE_PUBLISH', 'Approve and publish courses'),
    ('00000000-0000-0000-0001-000000000007', 'COURSE_ARCHIVE', 'Archive courses'),
    ('00000000-0000-0000-0001-000000000008', 'ASSIGNMENT_SUBMIT', 'Submit assignment answers'),
    ('00000000-0000-0000-0001-000000000009', 'ASSIGNMENT_GRADE', 'Grade student submissions'),
    ('00000000-0000-0000-0001-000000000010', 'QUIZ_ATTEMPT', 'Take quizzes and submit answers'),
    ('00000000-0000-0000-0001-000000000011', 'CHAT_MESSAGE', 'Send chat messages in enrolled course rooms'),
    ('00000000-0000-0000-0001-000000000012', 'USER_MANAGE', 'Manage users, roles and privileges'),
    ('00000000-0000-0000-0001-000000000013', 'PAYMENT_VIEW', 'View orders, payments and transactions'),
    ('00000000-0000-0000-0001-000000000014', 'AUDIT_VIEW', 'Inspect system audit logs');

-- Seed Data: Role Permissions
-- STUDENT
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000001'),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000002'),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000008'),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000010'),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0001-000000000011');

-- INSTRUCTOR
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000001'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000002'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000003'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000004'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000005'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000008'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000009'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000010'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0001-000000000011');

-- ADMIN & SUPER_ADMIN (Assign all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id FROM permissions;

INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permissions;

-- Seed Data: Initial Super Admin User (password: Admin@123)
-- BCrypt hash for Admin@123 is $2a$10$wE07vL4Z6R6i6yT.jVw0.e18m3jG4R6zOqU6rJz7F7rOaD2kLzT7m
INSERT INTO users (id, email, password_hash, full_name, status, email_verified_at)
VALUES (
    '00000000-0000-0000-0000-000000000099',
    'admin@enterprise-lms.com',
    '$2a$10$wE07vL4Z6R6i6yT.jVw0.e18m3jG4R6zOqU6rJz7F7rOaD2kLzT7m',
    'Super Admin',
    'ACTIVE',
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role_id)
VALUES (
    '00000000-0000-0000-0000-000000000099',
    '00000000-0000-0000-0000-000000000004'
);
