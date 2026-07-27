# Enterprise LMS + Collaboration Platform
## Software Requirements Specification & Complete Implementation Guide

**Document ID:** ELMS-SRS-001
**Version:** 1.0
**Status:** Baseline
**Prepared for:** Enterprise LMS + Collaboration Platform
**Based on:** Enterprise LMS Project Specification (source document)

---

## Document Control

| Field | Value |
|---|---|
| Document Title | Enterprise LMS + Collaboration Platform — SRS and Implementation Guide |
| Version | 1.0 |
| Classification | Internal / Project Documentation |
| Standard Followed | IEEE 830 (adapted) + ISO/IEC/IEEE 29148 |
| Audience | Architects, Backend Engineers, Frontend Engineers, DevOps, QA, Reviewers/Interviewers |

### Revision History

| Version | Date | Author | Description |
|---|---|---|---|
| 0.1 | — | Project Owner | Initial high-level specification (source deck) |
| 1.0 | — | Architecture Team | Expanded into full SRS, data model, API contract, DevOps and step-by-step build plan |

---

# 1. Introduction

## 1.1 Purpose

This document defines the complete software requirements for the **Enterprise LMS + Collaboration Platform** — a multi-tenant-ready Learning Management System with real-time collaboration, media streaming, payments and analytics, built on a Java microservices backend and a React frontend.

The document serves three purposes:

1. **Specification** — an unambiguous, testable statement of what the system must do (Sections 3–7).
2. **Design reference** — architecture, data model, API contract, security design (Sections 4, 6, 7, 9).
3. **Build guide** — a numbered, phase-by-phase implementation roadmap starting from Step 1 (Section 15), so a developer can execute it end to end without further design input.

## 1.2 Product Scope

The platform allows **Instructors** to author courses (modules → lessons → video/documents), publish them for free or for a fee, and assess learners through assignments and quizzes. **Students** discover courses, enrol (with payment where applicable), consume video content with resumable playback, submit assessments, chat in real time with peers and instructors, receive notifications, and earn verifiable certificates on completion. **Admins** and **Super Admins** manage users, roles, catalogue approval, payments reconciliation, and platform-wide analytics.

**In scope**

- Authentication, authorization and account lifecycle (JWT, refresh tokens, email verification, OTP, OAuth2, 2FA, RBAC)
- Course authoring, catalogue, categories, reviews, enrolment and progress tracking
- Video upload, transcoding hand-off, secure streaming, resume playback
- Assignments, submissions, grading; quizzes, question banks, auto-evaluation, results
- Real-time chat (course rooms and direct messages) with typing indicators and read receipts
- Notifications (in-app, real-time push over WebSocket, email)
- Payments, invoices, transactions, refunds
- Certificate generation and public verification
- Analytics dashboards for Instructor, Admin and Student
- Admin panel, audit logging, monitoring, CI/CD and cloud deployment

**Out of scope (v1)**

- Live video classes / WebRTC conferencing (planned — Section 18)
- Native mobile applications (planned)
- AI course assistant and recommendation engine (planned)
- SCORM/xAPI import-export, proctored examinations, offline mode

## 1.3 Definitions, Acronyms and Abbreviations

| Term | Meaning |
|---|---|
| **LMS** | Learning Management System |
| **RBAC** | Role-Based Access Control |
| **JWT** | JSON Web Token — signed, stateless access credential |
| **Refresh Token** | Long-lived, revocable, rotated token used to obtain new access tokens |
| **OTP** | One-Time Password, delivered by email, time-limited |
| **2FA** | Two-Factor Authentication (TOTP-based) |
| **OAuth2** | Delegated authorization protocol; used here for Google sign-in |
| **API Gateway** | Single entry point that routes, authenticates and rate-limits requests |
| **HLS** | HTTP Live Streaming — adaptive bitrate segmented video |
| **Presigned URL** | Time-limited, signed S3 URL granting direct object access |
| **STOMP** | Simple Text Oriented Messaging Protocol, used over WebSocket |
| **DLQ** | Dead Letter Queue |
| **Idempotency Key** | Client-supplied key ensuring a request is processed at most once |
| **SLO / SLA** | Service Level Objective / Agreement |
| **DTO** | Data Transfer Object |
| **CQRS** | Command Query Responsibility Segregation |
| **Saga** | Distributed transaction pattern using compensating actions |

## 1.4 References

1. Source specification: *Enterprise LMS Project Specification* (objectives, stack, modules, architecture, deployment pipeline)
2. IEEE 830-1998 — Recommended Practice for Software Requirements Specifications
3. ISO/IEC/IEEE 29148:2018 — Requirements Engineering
4. OWASP Application Security Verification Standard (ASVS) v4.0
5. RFC 7519 (JWT), RFC 6749 (OAuth 2.0), RFC 6238 (TOTP)
6. OpenAPI Specification 3.1
7. The Twelve-Factor App methodology

## 1.5 Document Conventions

- Requirement IDs use the form `FR-<MODULE>-<NN>` (functional) and `NFR-<CATEGORY>-<NN>` (non-functional).
- **Shall** = mandatory; **Should** = recommended; **May** = optional.
- Priority: **P0** (must have for v1), **P1** (should have), **P2** (nice to have).
- All timestamps are stored in UTC; all monetary values are stored as minor units (integer paise/cents) with an ISO-4217 currency code.

## 1.6 How to Read This Document

| If you are… | Read |
|---|---|
| Product owner / reviewer | Sections 1, 2, 3, 18, 19 |
| Backend engineer | Sections 4, 5, 6, 7, 9, 10, 15 |
| Frontend engineer | Sections 3, 5, 8, 10, 15 |
| DevOps engineer | Sections 4, 13, 14, 15, 16 |
| QA engineer | Sections 5, 11, 17, 19 |

---

# 2. Overall Description

## 2.1 Product Perspective

The platform is a **new, self-contained system** composed of independently deployable microservices behind an API Gateway, consumed by a React single-page application. It is not a replacement for an existing system, but it integrates with external providers:

| External System | Purpose | Integration Style |
|---|---|---|
| Google Identity Platform | OAuth2 social login | OIDC redirect + token exchange |
| SMTP provider (SES / SendGrid) | Verification mail, OTP, notifications | SMTP / REST API |
| Payment gateway (Razorpay / Stripe) | Checkout, capture, refund | REST + signed webhooks |
| AWS S3 + CloudFront | Media and document storage, CDN delivery | SDK, presigned URLs |
| Object transcoder (FFmpeg worker / MediaConvert) | Video → HLS renditions | Async job via queue |
| Prometheus / Grafana / ELK | Metrics, dashboards, log aggregation | Scrape + ship |

## 2.2 Product Functions (Summary)

1. **Identity & Access** — register, verify, log in, refresh, OAuth2, OTP, 2FA, reset password, RBAC, sessions, audit.
2. **User & Profile** — profiles, avatars, instructor applications, admin user management.
3. **Course Lifecycle** — create → draft → submit → approve → publish → archive; modules, lessons, resources, pricing, categories, tags.
4. **Discovery** — search, filters (category, level, price, rating, language), sorting, pagination, recommendations (v2).
5. **Enrolment & Progress** — free/paid enrolment, per-lesson progress, resume position, completion percentage.
6. **Media** — chunked upload, virus/type validation, transcoding, HLS playback with signed URLs, watch-time telemetry.
7. **Assessment** — assignments with file submission and manual grading; quizzes with timers, question banks, auto-scoring, attempt limits.
8. **Collaboration** — course chat rooms, direct messages, typing indicators, read receipts, message history, moderation.
9. **Notifications** — event-driven fan-out to in-app, WebSocket and email channels with user preferences.
10. **Commerce** — cart-less direct checkout, orders, payments, webhooks, invoices, refunds, coupons.
11. **Certification** — auto-issue on completion, PDF generation, unique verification code, public verify endpoint.
12. **Analytics** — student progress, instructor revenue and engagement, admin platform KPIs.
13. **Administration** — role and permission management, catalogue moderation, reports, audit log viewer, feature flags.

## 2.3 User Classes and Characteristics

| User Class | Description | Technical Skill | Frequency | Key Needs |
|---|---|---|---|---|
| **Guest** | Unauthenticated visitor | Low | High | Browse catalogue, view previews, register |
| **Student** | Enrolled learner | Low–Medium | Daily | Learn, resume video, submit work, chat, certificate |
| **Instructor** | Course author | Medium | Daily | Author content, upload video, grade, view earnings |
| **Admin** | Platform operator | High | Daily | Moderate content, manage users, resolve payments |
| **Super Admin** | Owner / root operator | High | Low | Manage admins, roles, permissions, system config |
| **System / Service Account** | Internal machine identity | — | Continuous | Service-to-service calls, scheduled jobs |

## 2.4 Operating Environment

| Layer | Requirement |
|---|---|
| Client | Chrome/Edge/Firefox/Safari — latest two major versions; screen widths 320 px → 2560 px |
| Runtime (backend) | Java 21 (LTS), Spring Boot 3.x, containerized on Linux x86-64 |
| Runtime (frontend) | Node.js 20 LTS build toolchain; static bundle served via CDN/Nginx |
| Datastores | PostgreSQL 16, Redis 7 |
| Messaging | RabbitMQ 3.13 (or Apache Kafka 3.x for high-volume event streaming) |
| Orchestration | Docker 25+, Docker Compose (local), Kubernetes 1.29+ (staging/prod) |
| Cloud | AWS (EC2/EKS, RDS, ElastiCache, S3, CloudFront, SES) — portable to Azure/GCP |

## 2.5 Design and Implementation Constraints

- **C-01** Backend shall be Java 21 + Spring Boot; no polyglot services in v1.
- **C-02** Each microservice owns its schema; **no cross-service database joins**. Cross-service data is obtained via API or replicated through events.
- **C-03** All external traffic shall pass through the API Gateway; services are not publicly routable.
- **C-04** All APIs shall be documented with OpenAPI 3.1 and served through Swagger UI in non-production profiles.
- **C-05** Stateless services only — session state lives in Redis or the token; enables horizontal scaling.
- **C-06** All configuration via environment variables (12-Factor); no secrets in source control.
- **C-07** Database changes shall be applied through versioned Flyway migrations, never by Hibernate `ddl-auto` in staging/production.
- **C-08** Media files shall never be proxied through application servers in production — use presigned URLs/CDN.
- **C-09** Payment amounts shall be verified server-side against the order record; client-supplied amounts are never trusted.
- **C-10** All timestamps stored as `TIMESTAMPTZ` in UTC.

## 2.6 Assumptions and Dependencies

- **A-01** A payment gateway merchant account is available for test and live modes.
- **A-02** An SMTP/SES sending domain with SPF/DKIM is configured before email verification goes live.
- **A-03** Transcoding is asynchronous; a lesson may be in `PROCESSING` state for several minutes after upload.
- **A-04** Initial capacity target is 10,000 registered users, 1,000 concurrent sessions, 200 concurrent video streams.
- **A-05** Single region deployment in v1; multi-region is a v2 concern.
- **D-01** Chat, notification and payment services depend on the message broker being available; degraded mode is defined in Section 12.

---

# 3. System Overview and Architecture

## 3.1 Architectural Style

The system uses a **three-tier architecture** (presentation → application → data), where the application tier is decomposed into **domain-aligned microservices**, and each service internally follows an **MVC / layered** structure:

```
Controller  →  Service (business rules)  →  Repository (Spring Data JPA / JDBC)  →  PostgreSQL
      ↘ DTO/Mapper        ↘ Event Publisher → RabbitMQ/Kafka        ↘ Redis cache
```

## 3.2 High-Level Component Diagram

```
                     ┌──────────────────────────────┐
                     │  React SPA (Tailwind, Redux) │
                     │  CloudFront + S3 static host │
                     └───────────────┬──────────────┘
                                     │ HTTPS / WSS
                     ┌───────────────▼──────────────┐
                     │        API GATEWAY           │
                     │ routing · JWT validation ·   │
                     │ rate limit · CORS · tracing  │
                     └──┬───┬───┬───┬───┬───┬───┬───┘
        ┌───────────────┘   │   │   │   │   │   └────────────────┐
        │        ┌──────────┘   │   │   │   └──────────┐         │
   ┌────▼────┐ ┌─▼─────┐ ┌──────▼─┐ ┌▼──────┐ ┌────────▼─┐ ┌─────▼─────┐ ┌──────────┐
   │  AUTH   │ │ USER  │ │ COURSE │ │ MEDIA │ │   CHAT   │ │NOTIFICATION│ │ PAYMENT │
   │ service │ │service│ │service │ │service│ │  service │ │  service   │ │ service │
   └────┬────┘ └───┬───┘ └────┬───┘ └───┬───┘ └────┬─────┘ └─────┬──────┘ └────┬────┘
        │          │          │         │          │             │             │
        └──────────┴────┬─────┴─────────┴──────────┴─────────────┴─────────────┘
                        │
        ┌───────────────┼────────────────┬─────────────────┬──────────────┐
   ┌────▼─────┐   ┌─────▼─────┐   ┌──────▼──────┐   ┌──────▼──────┐  ┌────▼─────┐
   │PostgreSQL│   │   Redis   │   │RabbitMQ/Kafka│  │   AWS S3    │  │   SES    │
   │ (schema  │   │ cache,    │   │ domain events│  │ + CloudFront│  │  email   │
   │ per svc) │   │ sessions, │   │  + DLQ       │  │  media/CDN  │  │          │
   │          │   │ rate limit│   │              │  │             │  │          │
   └──────────┘   └───────────┘   └─────────────┘   └─────────────┘  └──────────┘
```

## 3.3 Service Responsibility Matrix

| # | Service | Owns (data) | Publishes events | Consumes events | Port (local) |
|---|---|---|---|---|---|
| 1 | **api-gateway** | — | — | — | 8080 |
| 2 | **auth-service** | users(credentials), roles, permissions, otps, refresh_tokens, oauth_accounts | `user.registered`, `user.verified`, `user.password_reset` | — | 8081 |
| 3 | **user-service** | profiles, instructor_applications, preferences | `profile.updated` | `user.registered` | 8082 |
| 4 | **course-service** | courses, modules, lessons, categories, enrolments, progress, assignments, submissions, quizzes, questions, results, reviews, certificates | `course.published`, `enrolment.created`, `course.completed`, `assignment.graded` | `payment.succeeded`, `media.ready` | 8083 |
| 5 | **media-service** | media, upload_sessions, transcode_jobs | `media.uploaded`, `media.ready`, `media.failed` | — | 8084 |
| 6 | **chat-service** | chats, chat_members, messages, message_receipts | `chat.message.created` | `enrolment.created` (auto-join room) | 8085 |
| 7 | **notification-service** | notifications, notification_preferences, email_log | — | all domain events | 8086 |
| 8 | **payment-service** | orders, payments, transactions, invoices, refunds, coupons | `payment.succeeded`, `payment.failed`, `refund.completed` | `course.published` (price sync) | 8087 |
| 9 | **discovery/config** (optional) | service registry, central config | — | — | 8761 / 8888 |

## 3.4 Communication Patterns

| Pattern | Used for | Technology |
|---|---|---|
| Synchronous request/response | Client → Gateway → Service; read-time cross-service lookups | REST/JSON over HTTP, `WebClient`/OpenFeign |
| Asynchronous events | Side effects: notifications, enrolment on payment, chat room provisioning | RabbitMQ topic exchange (or Kafka topics) |
| Real-time push | Chat messages, typing, receipts, live notifications | WebSocket + STOMP, Redis pub/sub for multi-instance fan-out |
| Cache-aside | Course detail, catalogue pages, user profile, permission sets | Redis with TTL + explicit invalidation on write |
| Scheduled jobs | Certificate sweep, OTP purge, token cleanup, analytics rollup | Spring `@Scheduled` with ShedLock (single-fire across replicas) |

## 3.5 Request Lifecycle (Reference Flow)

1. Browser sends `GET /api/v1/courses/42` with `Authorization: Bearer <access-token>`.
2. Gateway checks the route, applies the rate-limit filter (Redis token bucket per user/IP), validates the JWT signature and expiry, injects `X-User-Id`, `X-Roles`, `X-Correlation-Id` headers.
3. Request is proxied to `course-service`.
4. `CourseController` binds the path variable, delegates to `CourseService`.
5. `CourseService` checks Redis for `course:42:detail`; on miss it loads the aggregate via `CourseRepository`, maps to `CourseDetailResponse`, caches it with a 10-minute TTL.
6. Authorization: `@PreAuthorize` verifies the caller may view an unpublished course (owner or admin only).
7. Response returns as `ApiResponse<CourseDetailResponse>`; the gateway adds security headers and returns to the client.
8. An access log entry with the correlation ID is emitted; metrics are recorded by Micrometer.

## 3.6 Environments

| Environment | Purpose | Data | Deploy trigger |
|---|---|---|---|
| `local` | Developer machine | Seeded sample data | Docker Compose, manual |
| `dev` | Integration | Synthetic | Push to `develop` |
| `staging` | Pre-production, UAT | Anonymized production-like | Merge to `release/*` |
| `prod` | Live | Real | Tag `v*.*.*` + manual approval |
---

# 4. Functional Requirements

Each requirement is testable and carries an ID, priority and acceptance criteria. Modules map 1:1 to the Core Modules listed in the source specification.

## 4.1 Module A — Authentication & User Management

### 4.1.1 Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-01 | The system shall allow registration with full name, email and password. Email must be unique (case-insensitive). | P0 |
| FR-AUTH-02 | Passwords shall be at least 8 characters with upper, lower, digit and symbol, and shall be stored using BCrypt (cost ≥ 10). | P0 |
| FR-AUTH-03 | On registration the system shall create the account in `PENDING_VERIFICATION` state and email a verification link valid for 24 hours. | P0 |
| FR-AUTH-04 | The system shall support verification by link **and** by 6-digit email OTP valid for 10 minutes, maximum 5 verification attempts per OTP. | P0 |
| FR-AUTH-05 | Login shall issue an access token (JWT, 15 min) and a refresh token (opaque or JWT, 7 days) bound to a device fingerprint. | P0 |
| FR-AUTH-06 | The refresh endpoint shall rotate refresh tokens; reuse of a consumed token shall revoke the entire token family and force re-login. | P0 |
| FR-AUTH-07 | Logout shall revoke the presented refresh token; "logout all devices" shall revoke every active token for the user. | P0 |
| FR-AUTH-08 | The system shall support Google OAuth2 login; a first-time OAuth user is auto-provisioned as a verified Student. | P0 |
| FR-AUTH-09 | If an OAuth email matches an existing local account, the accounts shall be linked after ownership confirmation. | P1 |
| FR-AUTH-10 | Forgot-password shall email a single-use reset token valid for 30 minutes; using it invalidates all sessions. | P0 |
| FR-AUTH-11 | The system shall support TOTP-based 2FA (enrol via QR/secret, verify, disable), plus 10 single-use recovery codes. | P1 |
| FR-AUTH-12 | When 2FA is enabled, login shall return a short-lived challenge token; tokens are issued only after successful TOTP validation. | P1 |
| FR-AUTH-13 | The system shall enforce RBAC with roles `STUDENT`, `INSTRUCTOR`, `ADMIN`, `SUPER_ADMIN` and fine-grained permissions. | P0 |
| FR-AUTH-14 | After 5 failed logins within 15 minutes the account shall be locked for 15 minutes and the user notified by email. | P0 |
| FR-AUTH-15 | Users shall be able to view active sessions (device, IP, last used) and revoke any of them. | P1 |
| FR-AUTH-16 | All authentication events shall be written to the audit log. | P0 |
| FR-AUTH-17 | Admins shall be able to create, suspend, reactivate and soft-delete users and to assign/revoke roles; role changes shall take effect within one access-token lifetime. | P0 |
| FR-AUTH-18 | Users shall be able to request account deletion; personal data shall be anonymized while transactional records are retained. | P2 |

### 4.1.2 Registration & Verification Flow

```
Client                 auth-service            notification-service       Mailbox
  │  POST /auth/register   │                            │                    │
  ├───────────────────────►│ validate, hash, save       │                    │
  │                        │ generate OTP + link        │                    │
  │                        ├── user.registered ────────►│ render template    │
  │                        │                            ├── SMTP ───────────►│
  │◄── 201 {userId, next}  │                            │                    │
  │  POST /auth/verify-otp │                            │                    │
  ├───────────────────────►│ check code, attempts, TTL  │                    │
  │                        │ status = ACTIVE            │                    │
  │◄── 200 {tokens}        │                            │                    │
```

### 4.1.3 Acceptance Criteria (sample)

- Registering with an existing email returns `409 EMAIL_ALREADY_EXISTS` and does not disclose whether the account is verified.
- An expired OTP returns `410 OTP_EXPIRED`; a wrong OTP increments the attempt counter and returns `400 OTP_INVALID`; the 6th attempt invalidates the OTP.
- Replaying a rotated refresh token returns `401 TOKEN_REUSE_DETECTED` and all sessions for that user are terminated.

## 4.2 Module B — Course Management

| ID | Requirement | Priority |
|---|---|---|
| FR-CRS-01 | Instructors shall create courses with title, slug, subtitle, description, category, level, language, thumbnail, price, currency and tags. | P0 |
| FR-CRS-02 | A course shall follow the lifecycle `DRAFT → PENDING_REVIEW → PUBLISHED → ARCHIVED` with `REJECTED` as a review outcome. | P0 |
| FR-CRS-03 | Courses shall contain ordered modules; modules shall contain ordered lessons of type `VIDEO`, `ARTICLE`, `QUIZ`, `ASSIGNMENT` or `RESOURCE`. | P0 |
| FR-CRS-04 | Reordering of modules/lessons shall be supported via a drag-and-drop-friendly bulk position update. | P1 |
| FR-CRS-05 | Instructors shall mark selected lessons as free previews accessible to guests. | P1 |
| FR-CRS-06 | Publishing shall be blocked unless the course has ≥ 1 module, ≥ 1 published-ready lesson, a thumbnail, a description ≥ 200 characters and a valid price. | P0 |
| FR-CRS-07 | Admins shall approve or reject submitted courses with a mandatory reason on rejection. | P0 |
| FR-CRS-08 | The catalogue shall support keyword search over title/subtitle/tags plus filters (category, level, language, price band, minimum rating) and sorting (relevance, newest, rating, popularity, price). | P0 |
| FR-CRS-09 | Catalogue responses shall be paginated (default 12, maximum 100 per page) and cached for 5 minutes. | P0 |
| FR-CRS-10 | Enrolled students shall submit exactly one review per course (rating 1–5 + comment); the course rating aggregate shall be recomputed on write. | P1 |
| FR-CRS-11 | Editing a published course shall create a draft revision; changes apply to learners only on re-publish. | P2 |
| FR-CRS-12 | Courses shall be soft-deleted; enrolled students retain access to archived courses. | P0 |

## 4.3 Module C — Enrolment & Progress

| ID | Requirement | Priority |
|---|---|---|
| FR-ENR-01 | Students shall enrol directly in free courses; paid courses require a successful payment event. | P0 |
| FR-ENR-02 | Duplicate enrolment attempts shall be rejected with `409 ALREADY_ENROLLED`. | P0 |
| FR-ENR-03 | The system shall record per-lesson progress: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, with `last_position_seconds` for video lessons. | P0 |
| FR-ENR-04 | Video progress shall be persisted at most every 15 seconds and on pause/unload; playback shall resume from the stored position ±2 seconds. | P0 |
| FR-ENR-05 | A video lesson shall auto-complete at ≥ 90 % watched. | P1 |
| FR-ENR-06 | Course completion percentage = completed lessons ÷ total gradeable lessons, recomputed on every progress write and cached. | P0 |
| FR-ENR-07 | At 100 % completion the system shall publish `course.completed` and trigger certificate issuance. | P0 |
| FR-ENR-08 | Sequential (drip) unlocking shall be optionally enforceable per course. | P2 |
| FR-ENR-09 | Students shall be able to unenrol from free courses; paid unenrolment follows the refund policy. | P2 |

## 4.4 Module D — Video & Media

| ID | Requirement | Priority |
|---|---|---|
| FR-MED-01 | Uploads shall use presigned, multipart S3 URLs; media bytes shall not transit the application servers. | P0 |
| FR-MED-02 | Accepted video types: MP4, MOV, MKV up to 2 GB; documents: PDF/DOCX/PPTX/ZIP up to 50 MB; images: JPG/PNG/WEBP up to 5 MB. | P0 |
| FR-MED-03 | Type shall be validated by MIME sniffing on the server side, not by file extension. | P0 |
| FR-MED-04 | On upload completion the media record enters `PROCESSING` and a transcode job is queued. | P0 |
| FR-MED-05 | Transcoding shall produce HLS renditions at 1080p/720p/480p/360p plus a thumbnail sprite; on success the record becomes `READY` and `media.ready` is published. | P1 |
| FR-MED-06 | Playback shall be authorized per request: only enrolled students, the owning instructor or an admin may obtain a signed manifest URL valid for 30 minutes. | P0 |
| FR-MED-07 | Failed jobs shall retry up to 3 times with exponential backoff, then move to `FAILED` and notify the instructor. | P1 |
| FR-MED-08 | Deleting a lesson shall schedule the associated S3 objects for deletion after a 30-day grace period. | P2 |
| FR-MED-09 | Watch telemetry (position, duration, quality, buffering events) shall be recorded for analytics. | P2 |

## 4.5 Module E — Assignments

| ID | Requirement | Priority |
|---|---|---|
| FR-ASG-01 | Instructors shall create assignments with title, instructions, attachment(s), max score, due date, late policy and allowed file types. | P0 |
| FR-ASG-02 | Students shall submit text and/or files before the due date; late submissions are flagged, and blocked if the policy forbids them. | P0 |
| FR-ASG-03 | Resubmission shall be allowed until the due date, retaining version history. | P1 |
| FR-ASG-04 | Instructors shall grade submissions with a score and feedback; grading publishes `assignment.graded` and notifies the student. | P0 |
| FR-ASG-05 | A submission shall progress through `SUBMITTED → UNDER_REVIEW → GRADED` (or `RETURNED` for rework). | P1 |
| FR-ASG-06 | Instructors shall export all submissions for an assignment as a ZIP/CSV. | P2 |

## 4.6 Module F — Quizzes

| ID | Requirement | Priority |
|---|---|---|
| FR-QZ-01 | Instructors shall build quizzes with settings: time limit, attempt limit, pass percentage, shuffle questions/options, show-answers policy. | P0 |
| FR-QZ-02 | Supported question types: single choice, multiple choice, true/false, short answer (exact/regex), numeric with tolerance. | P0 |
| FR-QZ-03 | Each question shall carry marks and optional negative marking; each option carries a correctness flag. | P0 |
| FR-QZ-04 | Correct answers shall never be included in the payload delivered to a student during an attempt. | P0 |
| FR-QZ-05 | Starting an attempt shall create a server-side timer; submission after expiry auto-submits the answers received so far. | P0 |
| FR-QZ-06 | Objective questions shall be auto-scored on submission; short-answer questions may be routed for manual review. | P0 |
| FR-QZ-07 | Results shall show score, percentage, pass/fail, per-question outcome (subject to the show-answers policy) and time taken. | P0 |
| FR-QZ-08 | Attempt limits shall be enforced server-side; the recorded grade is the best or last attempt per quiz configuration. | P0 |
| FR-QZ-09 | Answers shall be autosaved every 20 seconds so a disconnect does not lose progress. | P1 |

## 4.7 Module G — Real-time Chat

| ID | Requirement | Priority |
|---|---|---|
| FR-CHT-01 | Each published course shall have a chat room; enrolling auto-joins the student, unenrolling removes them. | P0 |
| FR-CHT-02 | One-to-one direct messages shall be supported between users who share a course. | P1 |
| FR-CHT-03 | Messages shall support text (≤ 4000 chars), emoji, attachments (≤ 10 MB) and replies to a parent message. | P0 |
| FR-CHT-04 | Delivery shall be real-time over WebSocket/STOMP with a REST fallback for history. | P0 |
| FR-CHT-05 | Typing indicators shall be broadcast to the room and expire after 3 seconds of inactivity. | P1 |
| FR-CHT-06 | Read receipts shall track delivered/read state per member; unread counts shall be available per room. | P1 |
| FR-CHT-07 | History shall be retrievable with cursor pagination (50 messages per page, newest first). | P0 |
| FR-CHT-08 | Senders may edit (15-minute window) or delete their messages; deleted messages show a tombstone. | P2 |
| FR-CHT-09 | Instructors and admins shall be able to mute members, pin messages and delete any message in their room. | P1 |
| FR-CHT-10 | Messages shall be rate-limited to 10 per 10 seconds per user; content shall be sanitized against XSS. | P0 |
| FR-CHT-11 | A user offline at send time shall receive a notification and see the message on reconnect. | P1 |

## 4.8 Module H — Notifications

| ID | Requirement | Priority |
|---|---|---|
| FR-NOT-01 | The service shall consume domain events and fan out to in-app, WebSocket and email channels. | P0 |
| FR-NOT-02 | Users shall configure per-category preferences (course updates, chat, assessment, payment, marketing) per channel. | P1 |
| FR-NOT-03 | In-app notifications shall support unread counts, mark-as-read, mark-all-read and pagination. | P0 |
| FR-NOT-04 | Email templates shall be versioned, localized-ready and rendered from a template engine (Thymeleaf/MJML). | P1 |
| FR-NOT-05 | Failed emails shall retry 3 times with backoff and then land in a DLQ with an alert. | P1 |
| FR-NOT-06 | Event handling shall be idempotent — duplicate events must not create duplicate notifications. | P0 |
| FR-NOT-07 | Marketing emails shall include an unsubscribe link honouring preference state. | P2 |

## 4.9 Module I — Payments

| ID | Requirement | Priority |
|---|---|---|
| FR-PAY-01 | Checkout shall create a server-side `Order` in `CREATED` state with the amount resolved from the course record. | P0 |
| FR-PAY-02 | The system shall integrate a gateway (Razorpay/Stripe) and never store raw card data (SAQ-A scope). | P0 |
| FR-PAY-03 | Payment confirmation shall be driven by verified webhooks with signature validation; client callbacks are advisory only. | P0 |
| FR-PAY-04 | Webhook processing shall be idempotent, keyed on the gateway event ID. | P0 |
| FR-PAY-05 | On `payment.succeeded`, enrolment shall be created automatically via the event bus; failure to enrol raises a compensating alert. | P0 |
| FR-PAY-06 | Orders unpaid after 30 minutes shall expire. | P1 |
| FR-PAY-07 | Coupons shall support percentage/flat discount, validity window, usage cap and per-user cap. | P2 |
| FR-PAY-08 | A GST/tax-compliant PDF invoice shall be generated and downloadable after successful payment. | P1 |
| FR-PAY-09 | Admins shall issue full or partial refunds within the policy window; refunds revoke access and publish `refund.completed`. | P1 |
| FR-PAY-10 | Instructor earnings shall be computed per transaction using the configured revenue-share percentage. | P1 |
| FR-PAY-11 | Every state change shall be recorded in an append-only transaction ledger. | P0 |

## 4.10 Module J — Certificates

| ID | Requirement | Priority |
|---|---|---|
| FR-CRT-01 | A certificate shall be issued automatically on 100 % course completion (and, where configured, a passing final quiz score). | P0 |
| FR-CRT-02 | Each certificate shall carry a unique verification code (UUID or ULID) and issue date. | P0 |
| FR-CRT-03 | A PDF shall be generated from an HTML template containing learner name, course title, instructor, hours, date, code and QR link. | P0 |
| FR-CRT-04 | `GET /certificates/verify/{code}` shall be public and return validity plus non-sensitive certificate details. | P0 |
| FR-CRT-05 | Certificates shall be immutable; corrections require revocation and reissue with an audit entry. | P1 |
| FR-CRT-06 | Certificates shall be downloadable and shareable via a public link (LinkedIn-compatible). | P1 |

## 4.11 Module K — Analytics Dashboards

| ID | Requirement | Priority |
|---|---|---|
| FR-ANL-01 | Students shall see courses in progress, completion percentages, streaks, hours watched, quiz averages and certificates. | P1 |
| FR-ANL-02 | Instructors shall see enrolments over time, completion funnel, average rating, revenue, and per-lesson drop-off. | P1 |
| FR-ANL-03 | Admins shall see DAU/MAU, new registrations, GMV, refunds, top courses, top instructors and system health. | P1 |
| FR-ANL-04 | All dashboards shall support date-range filters and CSV export. | P2 |
| FR-ANL-05 | Aggregates shall be precomputed by nightly rollup jobs into summary tables; dashboards read only from summaries. | P1 |

## 4.12 Module L — Admin Panel & Audit

| ID | Requirement | Priority |
|---|---|---|
| FR-ADM-01 | Admins shall manage users (search, filter, view, suspend, role change) with a full audit trail. | P0 |
| FR-ADM-02 | Admins shall moderate the course catalogue (approve, reject, feature, unpublish) and manage categories. | P0 |
| FR-ADM-03 | Super Admins shall manage roles and permission mappings; the last Super Admin cannot be removed. | P0 |
| FR-ADM-04 | The audit log shall record actor, action, entity type/ID, before/after snapshot, IP, user agent and timestamp; it shall be append-only. | P0 |
| FR-ADM-05 | Admins shall search and export audit entries by actor, entity, action and date range. | P1 |
| FR-ADM-06 | Feature flags shall allow enabling/disabling modules without redeployment. | P2 |

## 4.13 Role–Permission Matrix

| Capability | Guest | Student | Instructor | Admin | Super Admin |
|---|:--:|:--:|:--:|:--:|:--:|
| Browse catalogue / preview lessons | ✔ | ✔ | ✔ | ✔ | ✔ |
| Register / login | ✔ | — | — | — | — |
| Enrol, learn, submit, chat | — | ✔ | ✔ | ✔ | ✔ |
| Create/edit own course | — | — | ✔ | ✔ | ✔ |
| Upload media to own course | — | — | ✔ | ✔ | ✔ |
| Grade submissions (own course) | — | — | ✔ | ✔ | ✔ |
| Approve / reject / feature courses | — | — | — | ✔ | ✔ |
| Manage users & suspend accounts | — | — | — | ✔ | ✔ |
| Issue refunds | — | — | — | ✔ | ✔ |
| Manage roles & permissions | — | — | — | — | ✔ |
| View platform-wide analytics | — | — | — | ✔ | ✔ |
| View audit log | — | — | — | ✔ | ✔ |
| System configuration / feature flags | — | — | — | — | ✔ |

---

# 5. External Interface Requirements

## 5.1 User Interfaces

| Screen | Route | Primary users | Key elements |
|---|---|---|---|
| Landing / catalogue | `/` , `/courses` | Guest, Student | Search bar, filter sidebar, course grid, pagination |
| Course detail | `/courses/:slug` | All | Curriculum accordion, preview player, reviews, enrol/buy CTA |
| Checkout | `/checkout/:orderId` | Student | Order summary, coupon, gateway widget |
| Learning player | `/learn/:courseId/:lessonId` | Student | HLS player, curriculum sidebar, notes, resources, Q&A |
| Quiz runner | `/learn/:courseId/quiz/:quizId` | Student | Timer, question navigator, autosave indicator |
| Assignment submission | `/learn/:courseId/assignment/:id` | Student | Upload area, text editor, submission history |
| Chat | `/chat` , course drawer | Student, Instructor | Room list, message pane, typing indicator, unread badges |
| Student dashboard | `/dashboard` | Student | Continue learning, progress, certificates, notifications |
| Instructor studio | `/studio/*` | Instructor | Course builder, curriculum editor, uploads, grading queue, revenue |
| Admin console | `/admin/*` | Admin, Super Admin | Users, moderation queue, payments, audit, analytics, settings |
| Auth screens | `/login`, `/register`, `/verify`, `/forgot`, `/reset`, `/2fa` | All | Forms with inline validation, Google button, OTP entry |

**UI standards:** WCAG 2.1 AA contrast and keyboard navigation, responsive at 320/768/1024/1440 px breakpoints, skeleton loaders for all async regions, toast-based feedback, empty/error/loading states defined for every list, dark-mode-ready Tailwind theme tokens.

## 5.2 Software Interfaces

| Interface | Protocol / Contract | Failure handling |
|---|---|---|
| Gateway ↔ services | HTTP/1.1 JSON, OpenAPI 3.1 | Circuit breaker (Resilience4j), 3 s timeout, fallback response |
| Service ↔ PostgreSQL | JDBC + HikariCP, JPA/Hibernate | Pool max 20, 30 s connection timeout, retry on transient errors |
| Service ↔ Redis | Lettuce client | Cache miss on failure — never fail the request because of a cache outage |
| Service ↔ RabbitMQ | AMQP 0-9-1, topic exchange `lms.events` | Publisher confirms, 3 retries, DLQ `lms.events.dlq` |
| Service ↔ S3 | AWS SDK v2, presigned URLs | Exponential backoff, 5 retries |
| Service ↔ Payment gateway | HTTPS REST + HMAC-signed webhooks | Idempotent handlers, reconciliation job |
| Service ↔ SMTP/SES | SMTP over TLS or SES API | Queue-backed, retry, DLQ |

## 5.3 Communication Interfaces

- **HTTPS/TLS 1.2+** for all client traffic; HSTS enabled; HTTP redirects to HTTPS.
- **WSS** for WebSocket; handshake authenticated with the access token; heartbeat every 25 s; automatic reconnect with exponential backoff (1 s → 30 s cap).
- **CORS** restricted to configured front-end origins; credentials mode explicit.
- **Content-Type** `application/json; charset=utf-8` for APIs, `multipart/form-data` only where files bypass S3.

## 5.4 Standard Response Envelope

```json
{
  "success": true,
  "message": "Course fetched successfully",
  "data": { },
  "errors": null,
  "timestamp": "2026-01-01T10:15:30Z",
  "path": "/api/v1/courses/42",
  "correlationId": "0f9c1b2e-7a5e-4a1e-9d0b-2b7f9f8a11c3"
}
```

Error variant:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    { "field": "email", "code": "EMAIL_INVALID", "detail": "Must be a valid email address" }
  ],
  "timestamp": "2026-01-01T10:15:30Z",
  "path": "/api/v1/auth/register",
  "correlationId": "0f9c1b2e-7a5e-4a1e-9d0b-2b7f9f8a11c3"
}
```
---

# 6. Data Requirements and Database Design

## 6.1 Design Principles

1. **Schema per service.** `auth`, `users`, `course`, `media`, `chat`, `notify`, `pay` schemas (or separate databases in production). No foreign keys across service boundaries — only logical references by UUID.
2. **UUID v7 primary keys** (time-ordered) — safe to expose, index-friendly, merge-friendly across environments.
3. **Soft deletes** via `deleted_at TIMESTAMPTZ NULL` on user-visible entities; hard delete only through retention jobs.
4. **Auditing columns** on every table: `created_at`, `updated_at`, `created_by`, `updated_by`, `version` (optimistic locking).
5. **Money** stored as `BIGINT` minor units + `CHAR(3)` currency. Never `FLOAT`.
6. **Enums** stored as `VARCHAR` with a `CHECK` constraint (portable and migration-friendly) rather than native PG enums.
7. **Migrations** are forward-only Flyway scripts: `V1__baseline.sql`, `V2__add_quizzes.sql`, …

## 6.2 Entity Relationship Overview

```
USERS ─1:1─ PROFILES
USERS ─M:N─ ROLES ─M:N─ PERMISSIONS
USERS ─1:N─ REFRESH_TOKENS, OTPS, OAUTH_ACCOUNTS, AUDIT_LOGS

USERS(instructor) ─1:N─ COURSES ─1:N─ MODULES ─1:N─ LESSONS
COURSES ─N:1─ CATEGORIES            LESSONS ─0:1─ MEDIA
COURSES ─1:N─ ENROLMENTS ─N:1─ USERS(student)
ENROLMENTS ─1:N─ PROGRESS ─N:1─ LESSONS
COURSES ─1:N─ REVIEWS               COURSES ─1:N─ CERTIFICATES

LESSONS ─0:1─ ASSIGNMENTS ─1:N─ SUBMISSIONS
LESSONS ─0:1─ QUIZZES ─1:N─ QUESTIONS ─1:N─ OPTIONS
QUIZZES ─1:N─ QUIZ_ATTEMPTS ─1:N─ ATTEMPT_ANSWERS ─→ RESULTS

COURSES ─1:1─ CHATS ─1:N─ CHAT_MEMBERS
CHATS ─1:N─ MESSAGES ─1:N─ MESSAGE_RECEIPTS

USERS ─1:N─ ORDERS ─1:1─ PAYMENTS ─1:N─ TRANSACTIONS
PAYMENTS ─0:N─ REFUNDS              ORDERS ─0:1─ INVOICES
USERS ─1:N─ NOTIFICATIONS           USERS ─1:1─ NOTIFICATION_PREFERENCES
```

## 6.3 Auth Schema

### `users`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK | v7 |
| email | CITEXT | UNIQUE, NOT NULL | case-insensitive |
| password_hash | VARCHAR(100) | NULL | null for OAuth-only accounts |
| full_name | VARCHAR(120) | NOT NULL | |
| status | VARCHAR(30) | NOT NULL CHECK IN (PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED) | |
| email_verified_at | TIMESTAMPTZ | NULL | |
| two_factor_enabled | BOOLEAN | NOT NULL DEFAULT false | |
| two_factor_secret | VARCHAR(64) | NULL | encrypted at rest |
| failed_login_count | SMALLINT | NOT NULL DEFAULT 0 | |
| locked_until | TIMESTAMPTZ | NULL | |
| last_login_at | TIMESTAMPTZ | NULL | |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | |
| deleted_at | TIMESTAMPTZ | NULL | |
| version | BIGINT | NOT NULL DEFAULT 0 | |

Indexes: `UNIQUE(email) WHERE deleted_at IS NULL`, `idx_users_status`, `idx_users_created_at`.

### `roles`, `permissions`, `user_roles`, `role_permissions`

| Table | Key columns |
|---|---|
| `roles` | id UUID PK, name VARCHAR(40) UNIQUE (STUDENT/INSTRUCTOR/ADMIN/SUPER_ADMIN), description |
| `permissions` | id UUID PK, code VARCHAR(60) UNIQUE (e.g. `COURSE_PUBLISH`, `USER_SUSPEND`), description |
| `user_roles` | user_id FK, role_id FK, granted_by, granted_at — PK(user_id, role_id) |
| `role_permissions` | role_id FK, permission_id FK — PK(role_id, permission_id) |

### `refresh_tokens`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID NOT NULL | logical FK |
| token_hash | VARCHAR(128) NOT NULL | SHA-256 of the token; raw token never stored |
| family_id | UUID NOT NULL | rotation family for reuse detection |
| device_info | VARCHAR(255) | user agent digest |
| ip_address | INET | |
| expires_at | TIMESTAMPTZ NOT NULL | |
| revoked_at | TIMESTAMPTZ NULL | |
| replaced_by | UUID NULL | next token in the chain |

Indexes: `UNIQUE(token_hash)`, `idx_rt_user_active(user_id) WHERE revoked_at IS NULL`, `idx_rt_expires_at`.

### `otps`

`id`, `user_id`, `purpose` (EMAIL_VERIFICATION | LOGIN_2FA | PASSWORD_RESET | EMAIL_CHANGE), `code_hash`, `attempts SMALLINT`, `max_attempts SMALLINT DEFAULT 5`, `expires_at`, `consumed_at`. Index: `idx_otp_user_purpose_active`.

### `oauth_accounts`

`id`, `user_id`, `provider` (GOOGLE), `provider_user_id`, `email`, `linked_at` — `UNIQUE(provider, provider_user_id)`.

### `audit_logs`

`id`, `actor_id`, `actor_role`, `action`, `entity_type`, `entity_id`, `before JSONB`, `after JSONB`, `ip_address INET`, `user_agent`, `correlation_id`, `created_at`. Append-only (revoke UPDATE/DELETE), monthly partitions, indexes on `(entity_type, entity_id)`, `(actor_id, created_at DESC)`.

## 6.4 User Schema

### `profiles`

`user_id UUID PK`, `headline VARCHAR(160)`, `bio TEXT`, `avatar_url`, `phone`, `country`, `timezone`, `language`, `website`, `linkedin`, `github`, `expertise_tags TEXT[]`, `is_public BOOLEAN`.

### `instructor_applications`

`id`, `user_id`, `status` (PENDING/APPROVED/REJECTED), `qualifications TEXT`, `sample_url`, `reviewed_by`, `review_note`, `reviewed_at`.

## 6.5 Course Schema

### `categories`

`id`, `name`, `slug UNIQUE`, `parent_id NULL` (self-reference for sub-categories), `icon`, `display_order`, `is_active`.

### `courses`

| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| instructor_id | UUID NOT NULL | logical FK → users |
| category_id | UUID NOT NULL | FK |
| title | VARCHAR(160) NOT NULL | |
| slug | VARCHAR(180) UNIQUE NOT NULL | |
| subtitle | VARCHAR(255) | |
| description | TEXT NOT NULL | |
| level | VARCHAR(20) CHECK IN (BEGINNER, INTERMEDIATE, ADVANCED, ALL) | |
| language | VARCHAR(10) DEFAULT 'en' | |
| thumbnail_url | VARCHAR(500) | |
| promo_video_id | UUID NULL | → media |
| price_minor | BIGINT NOT NULL DEFAULT 0 | 0 = free |
| currency | CHAR(3) DEFAULT 'INR' | |
| discount_price_minor | BIGINT NULL | |
| status | VARCHAR(20) CHECK IN (DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, ARCHIVED) | |
| rating_avg | NUMERIC(3,2) DEFAULT 0 | denormalized |
| rating_count | INT DEFAULT 0 | denormalized |
| enrolment_count | INT DEFAULT 0 | denormalized |
| total_duration_seconds | INT DEFAULT 0 | denormalized |
| tags | TEXT[] | GIN indexed |
| requirements / outcomes | TEXT[] | |
| published_at, rejected_reason | | |

Indexes: `idx_courses_status_published_at`, `idx_courses_category`, `idx_courses_instructor`, `GIN(tags)`, `GIN(to_tsvector('english', title || ' ' || subtitle))` for full-text search.

### `modules`

`id`, `course_id FK`, `title`, `description`, `position INT NOT NULL`, `UNIQUE(course_id, position) DEFERRABLE`.

### `lessons`

`id`, `module_id FK`, `course_id` (denormalized for fast filtering), `title`, `type` (VIDEO/ARTICLE/QUIZ/ASSIGNMENT/RESOURCE), `content TEXT` (article body), `media_id UUID NULL`, `duration_seconds INT`, `position INT`, `is_free_preview BOOLEAN`, `is_published BOOLEAN`.

### `enrolments`

`id`, `course_id`, `student_id`, `source` (FREE/PURCHASE/ADMIN_GRANT), `order_id NULL`, `status` (ACTIVE/COMPLETED/CANCELLED/REFUNDED), `progress_percent NUMERIC(5,2) DEFAULT 0`, `enrolled_at`, `completed_at`, `last_accessed_at` — `UNIQUE(course_id, student_id)`.

### `lesson_progress`

`id`, `enrolment_id FK`, `lesson_id`, `status` (NOT_STARTED/IN_PROGRESS/COMPLETED), `last_position_seconds INT DEFAULT 0`, `watched_seconds INT DEFAULT 0`, `completed_at` — `UNIQUE(enrolment_id, lesson_id)`, index on `(enrolment_id, status)`.

### `assignments` / `submissions`

- `assignments`: `id`, `lesson_id`, `course_id`, `title`, `instructions TEXT`, `max_score INT`, `due_at`, `allow_late BOOLEAN`, `late_penalty_percent INT`, `allowed_file_types TEXT[]`, `max_file_size_mb INT`.
- `submissions`: `id`, `assignment_id`, `student_id`, `attempt_no INT`, `text_answer TEXT`, `file_urls TEXT[]`, `status` (SUBMITTED/UNDER_REVIEW/GRADED/RETURNED), `is_late BOOLEAN`, `score NUMERIC(6,2)`, `feedback TEXT`, `graded_by`, `graded_at`, `submitted_at` — `UNIQUE(assignment_id, student_id, attempt_no)`.

### `quizzes`, `questions`, `question_options`, `quiz_attempts`, `attempt_answers`, `quiz_results`

- `quizzes`: `id`, `lesson_id`, `course_id`, `title`, `description`, `time_limit_minutes INT`, `max_attempts INT DEFAULT 1`, `pass_percent NUMERIC(5,2) DEFAULT 60`, `shuffle_questions BOOLEAN`, `shuffle_options BOOLEAN`, `show_answers_policy` (NEVER/AFTER_SUBMIT/AFTER_DUE), `total_marks NUMERIC(6,2)`, `is_published`.
- `questions`: `id`, `quiz_id`, `type` (SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/SHORT_ANSWER/NUMERIC), `text TEXT`, `marks NUMERIC(5,2)`, `negative_marks NUMERIC(5,2) DEFAULT 0`, `explanation TEXT`, `position`, `correct_text` (short answer), `tolerance NUMERIC` (numeric type).
- `question_options`: `id`, `question_id`, `text`, `is_correct BOOLEAN`, `position`.
- `quiz_attempts`: `id`, `quiz_id`, `student_id`, `attempt_no`, `started_at`, `expires_at`, `submitted_at`, `status` (IN_PROGRESS/SUBMITTED/AUTO_SUBMITTED/EXPIRED) — `UNIQUE(quiz_id, student_id, attempt_no)`.
- `attempt_answers`: `id`, `attempt_id`, `question_id`, `selected_option_ids UUID[]`, `text_answer`, `numeric_answer`, `is_correct BOOLEAN NULL`, `marks_awarded NUMERIC(6,2)`, `answered_at`.
- `quiz_results`: `id`, `attempt_id UNIQUE`, `score NUMERIC(6,2)`, `percentage NUMERIC(5,2)`, `passed BOOLEAN`, `correct_count`, `wrong_count`, `unanswered_count`, `time_taken_seconds`, `evaluated_at`.

### `reviews`

`id`, `course_id`, `student_id`, `rating SMALLINT CHECK 1..5`, `comment TEXT`, `is_approved BOOLEAN DEFAULT true`, `created_at` — `UNIQUE(course_id, student_id)`.

### `certificates`

`id`, `course_id`, `student_id`, `enrolment_id`, `verification_code VARCHAR(40) UNIQUE`, `pdf_url`, `issued_at`, `revoked_at NULL`, `revoke_reason` — `UNIQUE(course_id, student_id) WHERE revoked_at IS NULL`.

## 6.6 Media Schema

### `media`

`id`, `owner_id`, `context_type` (COURSE_VIDEO/PROMO/THUMBNAIL/ASSIGNMENT/CHAT_ATTACHMENT/AVATAR), `context_id`, `original_filename`, `mime_type`, `size_bytes BIGINT`, `storage_key`, `cdn_url`, `hls_manifest_url`, `duration_seconds`, `width`, `height`, `status` (PENDING/UPLOADED/PROCESSING/READY/FAILED), `checksum_sha256`, `created_at`.

### `upload_sessions`

`id`, `media_id`, `upload_id` (S3 multipart), `parts JSONB`, `expires_at`, `completed_at`.

### `transcode_jobs`

`id`, `media_id`, `status`, `attempt SMALLINT`, `renditions JSONB`, `error TEXT`, `started_at`, `finished_at`.

## 6.7 Chat Schema

- `chats`: `id`, `type` (COURSE_ROOM/DIRECT/GROUP), `course_id NULL`, `title`, `created_by`, `is_archived`.
- `chat_members`: `chat_id`, `user_id`, `role` (MEMBER/MODERATOR/OWNER), `joined_at`, `muted_until`, `last_read_message_id`, `unread_count INT` — PK(chat_id, user_id).
- `messages`: `id` (ULID for ordering), `chat_id`, `sender_id`, `parent_id NULL`, `content TEXT`, `attachments JSONB`, `is_edited`, `edited_at`, `deleted_at`, `created_at` — index `(chat_id, created_at DESC)`, partitioned monthly at scale.
- `message_receipts`: `message_id`, `user_id`, `delivered_at`, `read_at` — PK(message_id, user_id).

## 6.8 Notification Schema

- `notifications`: `id`, `user_id`, `category` (COURSE/CHAT/ASSESSMENT/PAYMENT/SYSTEM), `title`, `body`, `action_url`, `metadata JSONB`, `is_read BOOLEAN`, `read_at`, `created_at` — index `(user_id, is_read, created_at DESC)`.
- `notification_preferences`: `user_id PK`, `email_enabled JSONB`, `in_app_enabled JSONB`, `push_enabled JSONB`, `digest_frequency`.
- `email_log`: `id`, `to_email`, `template`, `subject`, `status` (QUEUED/SENT/FAILED/BOUNCED), `provider_message_id`, `error`, `attempts`, `sent_at`.
- `processed_events`: `event_id PK`, `handler`, `processed_at` — idempotency guard.

## 6.9 Payment Schema

- `orders`: `id`, `user_id`, `course_id`, `amount_minor`, `discount_minor`, `tax_minor`, `total_minor`, `currency`, `coupon_code`, `status` (CREATED/PENDING/PAID/FAILED/EXPIRED/REFUNDED), `gateway_order_id`, `expires_at`, `created_at`.
- `payments`: `id`, `order_id UNIQUE`, `gateway` (RAZORPAY/STRIPE), `gateway_payment_id UNIQUE`, `method` (CARD/UPI/NETBANKING/WALLET), `amount_minor`, `status` (AUTHORIZED/CAPTURED/FAILED/REFUNDED), `captured_at`, `failure_code`, `raw_payload JSONB`.
- `transactions`: `id`, `payment_id`, `type` (CHARGE/REFUND/PAYOUT/FEE), `amount_minor`, `balance_after_minor`, `note`, `created_at` — append-only ledger.
- `invoices`: `id`, `order_id UNIQUE`, `invoice_number UNIQUE`, `pdf_url`, `tax_breakup JSONB`, `issued_at`.
- `refunds`: `id`, `payment_id`, `amount_minor`, `reason`, `status`, `gateway_refund_id`, `initiated_by`, `processed_at`.
- `coupons`: `id`, `code UNIQUE`, `type` (PERCENT/FLAT), `value`, `max_uses`, `used_count`, `per_user_limit`, `valid_from`, `valid_until`, `applicable_course_ids UUID[]`, `is_active`.
- `webhook_events`: `id`, `gateway`, `event_id UNIQUE`, `type`, `payload JSONB`, `signature_valid BOOLEAN`, `processed_at`, `error`.

## 6.10 Analytics (Summary Tables)

- `daily_course_stats`: `date`, `course_id`, `views`, `enrolments`, `completions`, `revenue_minor`, `watch_seconds` — PK(date, course_id).
- `daily_platform_stats`: `date`, `dau`, `new_users`, `gmv_minor`, `refunds_minor`, `active_courses`.
- `lesson_dropoff`: `lesson_id`, `date`, `starts`, `completions`, `avg_watch_percent`.

## 6.11 Redis Key Design

| Key pattern | Type | TTL | Purpose |
|---|---|---|---|
| `auth:blacklist:{jti}` | string | token TTL | revoked access tokens |
| `auth:otp:rate:{email}` | counter | 15 min | OTP request throttle |
| `rl:{route}:{userOrIp}` | token bucket | 1 min | gateway rate limiting |
| `course:detail:{id}` | JSON string | 10 min | course aggregate cache |
| `catalog:{filtersHash}:{page}` | JSON string | 5 min | catalogue page cache |
| `perm:{userId}` | set | 30 min | resolved permission set |
| `chat:typing:{chatId}` | hash | 5 s | typing indicators |
| `chat:presence:{userId}` | string | 60 s | online presence heartbeat |
| `ws:session:{userId}` | set | session | connected node IDs for fan-out |
| `quiz:attempt:{attemptId}` | hash | attempt TTL | server-side timer & autosave buffer |

## 6.12 Data Retention & Privacy

| Data | Retention | Rule |
|---|---|---|
| OTPs | 24 h | purged by scheduled job |
| Expired/revoked refresh tokens | 30 days | purged |
| Chat messages | 24 months | archived to cold storage |
| Audit logs | 7 years | partitioned, immutable |
| Payment records | 7 years | statutory |
| Deleted accounts | 30-day grace, then anonymized | email → `deleted+{uuid}@…`, PII nulled, financial rows retained |

---

# 7. API Specification

Base path: `/api/v1`. All endpoints return the envelope from Section 5.4. Authenticated endpoints require `Authorization: Bearer <token>`.

## 7.1 Auth Service

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | — | Create account, send verification |
| POST | `/auth/verify-email` | — | Verify by link token |
| POST | `/auth/otp/send` | — | Send OTP (purpose in body) |
| POST | `/auth/otp/verify` | — | Verify OTP |
| POST | `/auth/login` | — | Password login → tokens or 2FA challenge |
| POST | `/auth/2fa/verify` | challenge | Verify TOTP → tokens |
| POST | `/auth/refresh` | refresh token | Rotate and issue new tokens |
| POST | `/auth/logout` | ✔ | Revoke current refresh token |
| POST | `/auth/logout-all` | ✔ | Revoke all sessions |
| POST | `/auth/forgot-password` | — | Email reset link |
| POST | `/auth/reset-password` | reset token | Set new password |
| POST | `/auth/change-password` | ✔ | Old + new password |
| GET | `/auth/oauth2/google` | — | Redirect to Google |
| GET | `/auth/oauth2/callback/google` | — | Exchange code → tokens |
| POST | `/auth/2fa/setup` / `/auth/2fa/enable` / `/auth/2fa/disable` | ✔ | TOTP lifecycle |
| GET | `/auth/sessions` · DELETE `/auth/sessions/{id}` | ✔ | List / revoke sessions |
| GET | `/auth/me` | ✔ | Current identity, roles, permissions |
| GET/POST/PATCH/DELETE | `/admin/users*`, `/admin/roles*` | ADMIN+ | User & role administration |

**Sample — `POST /auth/login`**

```json
// request
{ "email": "riya@example.com", "password": "P@ssw0rd!", "deviceInfo": "Chrome/126 macOS" }

// 200 response
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "b7f1c0a2-...-9d3e",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": "018f...", "fullName": "Riya S", "roles": ["STUDENT"], "avatarUrl": null }
  }
}

// 200 response when 2FA is on
{ "success": true, "data": { "twoFactorRequired": true, "challengeToken": "…", "expiresIn": 300 } }
```

**JWT claims**

```json
{
  "sub": "018f2c3d-...-a91b",
  "email": "riya@example.com",
  "roles": ["STUDENT"],
  "perms": ["COURSE_VIEW", "ENROL_CREATE"],
  "jti": "0c2a...",
  "iss": "lms-auth",
  "aud": "lms-api",
  "iat": 1767225600,
  "exp": 1767226500
}
```

## 7.2 Course Service

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/courses` | — | Catalogue: `q, category, level, language, minRating, priceMin, priceMax, sort, page, size` |
| GET | `/courses/{slug}` | optional | Public detail (curriculum with locked flags) |
| POST | `/courses` | INSTRUCTOR | Create draft |
| PUT/PATCH | `/courses/{id}` | owner/ADMIN | Update |
| POST | `/courses/{id}/submit` | owner | Send for review |
| POST | `/courses/{id}/approve` · `/reject` | ADMIN | Moderation |
| POST | `/courses/{id}/publish` · `/archive` | owner/ADMIN | Lifecycle |
| POST | `/courses/{id}/modules` · PATCH `/modules/{id}` · DELETE | owner | Module CRUD |
| PATCH | `/courses/{id}/curriculum/reorder` | owner | Bulk position update |
| POST | `/modules/{id}/lessons` · PATCH/DELETE `/lessons/{id}` | owner | Lesson CRUD |
| GET | `/lessons/{id}/playback` | enrolled | Signed HLS manifest URL |
| POST | `/courses/{id}/enrol` | STUDENT | Free enrolment |
| GET | `/me/enrolments` | ✔ | My courses |
| PUT | `/enrolments/{id}/progress` | ✔ | `{ lessonId, positionSeconds, watchedSeconds, completed }` |
| GET | `/enrolments/{id}/progress` | ✔ | Progress map + percentage |
| POST/GET | `/courses/{id}/reviews` | ✔ / — | Create / list reviews |
| POST | `/lessons/{id}/assignments` … | owner | Assignment CRUD |
| POST | `/assignments/{id}/submissions` | STUDENT | Submit |
| GET | `/assignments/{id}/submissions` | owner | Grading queue |
| POST | `/submissions/{id}/grade` | owner | `{ score, feedback }` |
| POST | `/lessons/{id}/quizzes` … | owner | Quiz & question CRUD |
| POST | `/quizzes/{id}/attempts` | STUDENT | Start attempt (returns questions without answers) |
| PATCH | `/attempts/{id}/answers` | STUDENT | Autosave |
| POST | `/attempts/{id}/submit` | STUDENT | Submit → auto-evaluate |
| GET | `/attempts/{id}/result` | STUDENT/owner | Result detail |
| GET | `/me/certificates` · GET `/certificates/verify/{code}` | ✔ / public | Certificates |
| GET | `/analytics/instructor/{courseId}` · `/analytics/admin/overview` | role-gated | Dashboards |

**Sample — progress update**

```json
PUT /api/v1/enrolments/018f…/progress
{ "lessonId": "018f…", "positionSeconds": 742, "watchedSeconds": 690, "completed": false }

// 200
{ "success": true, "data": { "lessonStatus": "IN_PROGRESS", "coursePercent": 38.46, "nextLessonId": "018f…" } }
```

## 7.3 Media Service

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/media/upload-url` | INSTRUCTOR | `{filename, mimeType, sizeBytes, contextType, contextId}` → `{mediaId, uploadId, partUrls[]}` |
| POST | `/media/{id}/complete` | INSTRUCTOR | Finalize multipart, enqueue transcode |
| GET | `/media/{id}` | role-gated | Metadata + status |
| GET | `/media/{id}/stream` | enrolled | Signed manifest URL (30 min) |
| DELETE | `/media/{id}` | owner/ADMIN | Soft delete |

## 7.4 Chat Service

| Method | Path | Description |
|---|---|---|
| GET | `/chats` | My rooms with unread counts |
| GET | `/chats/{id}/messages?before={ulid}&limit=50` | Cursor-paginated history |
| POST | `/chats/direct` | Open/create a DM |
| PATCH | `/messages/{id}` · DELETE | Edit / delete own message |
| POST | `/chats/{id}/read` | `{ lastReadMessageId }` |
| POST | `/chats/{id}/members/{userId}/mute` | Moderator action |

**STOMP topology**

| Destination | Direction | Payload |
|---|---|---|
| `/app/chat.send` | client → server | `{ chatId, content, parentId?, tempId }` |
| `/app/chat.typing` | client → server | `{ chatId, typing: true }` |
| `/app/chat.read` | client → server | `{ chatId, messageId }` |
| `/topic/chat.{chatId}` | server → clients | new/edited/deleted message |
| `/topic/chat.{chatId}.typing` | server → clients | `{ userId, typing }` |
| `/user/queue/notifications` | server → user | notification payload |
| `/user/queue/receipts` | server → user | delivery/read updates |

## 7.5 Payment Service

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/orders` | ✔ | `{ courseId, couponCode? }` → order + gateway order ID |
| GET | `/orders/{id}` | owner | Order status |
| POST | `/payments/verify` | ✔ | Client-side confirmation (advisory) |
| POST | `/webhooks/{gateway}` | signature | Authoritative payment events |
| GET | `/me/orders` · `/me/invoices/{id}` | ✔ | History, invoice PDF |
| POST | `/admin/refunds` | ADMIN | Initiate refund |
| GET | `/admin/payments` | ADMIN | Search / reconcile |

## 7.6 Notification Service

| Method | Path | Description |
|---|---|---|
| GET | `/notifications?unreadOnly=true&page=0` | List |
| GET | `/notifications/unread-count` | Badge count |
| POST | `/notifications/{id}/read` · `/notifications/read-all` | Mark read |
| GET/PUT | `/notifications/preferences` | Per-category, per-channel settings |

## 7.7 HTTP Status & Error Code Catalogue

| Status | When | Example codes |
|---|---|---|
| 200 / 201 / 204 | Success / created / no content | — |
| 400 | Validation failure | `VALIDATION_ERROR`, `OTP_INVALID`, `INVALID_STATE_TRANSITION` |
| 401 | Missing/expired/invalid token | `TOKEN_EXPIRED`, `TOKEN_REUSE_DETECTED`, `BAD_CREDENTIALS` |
| 403 | Authenticated but not permitted | `ACCESS_DENIED`, `NOT_ENROLLED`, `EMAIL_NOT_VERIFIED` |
| 404 | Resource absent | `COURSE_NOT_FOUND`, `LESSON_NOT_FOUND` |
| 409 | Conflict | `EMAIL_ALREADY_EXISTS`, `ALREADY_ENROLLED`, `DUPLICATE_REVIEW` |
| 410 | Gone / expired | `OTP_EXPIRED`, `ORDER_EXPIRED`, `ATTEMPT_EXPIRED` |
| 413 | Payload too large | `FILE_TOO_LARGE` |
| 415 | Unsupported media | `UNSUPPORTED_FILE_TYPE` |
| 422 | Semantically invalid | `INSUFFICIENT_COURSE_CONTENT` |
| 429 | Throttled | `RATE_LIMIT_EXCEEDED` (with `Retry-After`) |
| 500 / 503 | Server / dependency failure | `INTERNAL_ERROR`, `SERVICE_UNAVAILABLE` |

## 7.8 API Conventions

- **Versioning:** URI-based `/api/v1`; breaking changes create `/v2` with a 6-month deprecation window announced through the `Sunset` header.
- **Pagination:** `page` (0-based), `size` (default 20, max 100); responses include `totalElements`, `totalPages`, `first`, `last`. Chat and feeds use cursor pagination.
- **Filtering & sorting:** `sort=field,asc|desc` (repeatable). Unknown sort fields return `400`.
- **Idempotency:** `Idempotency-Key` header required on `POST /orders` and refund endpoints; replays return the original response.
- **Correlation:** every request carries `X-Correlation-Id` (generated at the gateway if absent) and it is echoed in responses and logs.
- **Documentation:** each service exposes `/v3/api-docs`; the gateway aggregates them into one Swagger UI at `/swagger-ui.html` (disabled in production).
---

# 8. Frontend Design Specification

## 8.1 Stack and Structure

React 18 + Vite + TypeScript, Tailwind CSS, Redux Toolkit (with RTK Query for server state), React Router v6, Axios (for non-RTKQ calls and interceptors), React Hook Form + Zod for validation, hls.js for adaptive playback, `@stomp/stompjs` + SockJS for real-time, Recharts for analytics, i18next for future localization.

```
frontend/src/
├── app/                # store.ts, rootReducer, router.tsx, providers
├── features/
│   ├── auth/           # slices, api, pages (Login, Register, Verify, Reset, TwoFactor)
│   ├── catalog/        # CourseList, CourseCard, Filters, CourseDetail
│   ├── learning/       # Player, CurriculumSidebar, Notes, ProgressTracker
│   ├── assessment/     # QuizRunner, QuestionNav, AssignmentSubmit
│   ├── chat/           # ChatLayout, RoomList, MessagePane, TypingIndicator
│   ├── notifications/  # Bell, NotificationList, Preferences
│   ├── payments/       # Checkout, OrderStatus, Invoices
│   ├── studio/         # instructor: CourseBuilder, CurriculumEditor, Uploader, GradingQueue
│   └── admin/          # Users, Moderation, Payments, AuditLog, Settings
├── components/ui/      # Button, Input, Modal, Table, Toast, Skeleton, EmptyState
├── hooks/              # useAuth, useWebSocket, useDebounce, useInfiniteScroll, useMediaQuery
├── lib/                # axios.ts, stomp.ts, storage.ts, formatters, constants
├── types/              # shared DTO types mirroring the OpenAPI schema
└── styles/             # tailwind.css, theme tokens
```

## 8.2 State Management Rules

| State kind | Where it lives |
|---|---|
| Server data (courses, progress, chat history) | RTK Query cache with tag-based invalidation |
| Session (user, roles, tokens) | `authSlice` + access token in memory, refresh token in an httpOnly cookie |
| UI state (modals, sidebars, filters) | local `useState` or a small `uiSlice` |
| Real-time streams (messages, typing, notifications) | dedicated slices updated by the STOMP listener, merged into RTKQ cache via `updateQueryData` |

## 8.3 Token Handling

1. Access token is kept **in memory only** (never `localStorage`) to reduce XSS impact.
2. Refresh token is stored in an `httpOnly; Secure; SameSite=Strict` cookie set by the auth service.
3. An Axios response interceptor catches `401 TOKEN_EXPIRED`, calls `/auth/refresh` **once** (with a shared in-flight promise so parallel 401s queue), retries the original requests, and on refresh failure clears state and redirects to `/login?next=…`.
4. Route guards: `<ProtectedRoute roles={['INSTRUCTOR']}>` renders `403` for insufficient roles and preserves the intended destination for post-login redirect.

## 8.4 Video Player Requirements

- hls.js with native fallback on Safari; quality selector plus `AUTO`.
- Playback speeds 0.5×–2×, captions track support, keyboard shortcuts (space, ←/→ 10 s, F, M).
- Resume prompt: "Resume from 12:22" when a stored position > 30 s exists.
- Progress emitter: throttled to one write per 15 s, plus writes on pause, seek-end, `visibilitychange` and `beforeunload` (via `navigator.sendBeacon`).
- Autoplay next lesson (user-toggleable); mark complete at ≥ 90 %.

## 8.5 Performance Budget

| Metric | Target |
|---|---|
| Initial JS bundle (gzipped) | ≤ 250 KB |
| Largest Contentful Paint (4G, mid-tier mobile) | ≤ 2.5 s |
| Time to Interactive | ≤ 3.5 s |
| Cumulative Layout Shift | ≤ 0.1 |
| Lighthouse performance / a11y | ≥ 90 / ≥ 95 |

Techniques: route-level code splitting (`React.lazy`), image lazy-loading with `srcset`, prefetch on link hover, memoized list rows, virtualized long lists (chat, submissions), Tailwind JIT purge, CDN caching with long-lived hashed filenames.

---

# 9. Security Requirements and Design

## 9.1 Authentication Design

| Aspect | Decision |
|---|---|
| Password hashing | BCrypt, strength 12; upgrade-on-login if the stored cost is lower |
| Access token | RS256-signed JWT, 15 min, claims per §7.1; public key exposed at the gateway via JWKS |
| Refresh token | Opaque 256-bit random value; only its SHA-256 hash is stored; rotated on every use |
| Reuse detection | Rotation family — presenting a consumed token revokes the whole family |
| OTP | 6 digits, hashed, 10-minute TTL, 5 attempts, 60-second resend cooldown, max 5 per hour per email |
| 2FA | TOTP (RFC 6238), 30-second step, ±1 window drift, 10 single-use recovery codes (hashed) |
| OAuth2 | Authorization Code + PKCE, `state` nonce validated, ID token signature and `aud`/`iss` verified |
| Logout | Refresh token revoked; access token `jti` added to the Redis blacklist until natural expiry |

## 9.2 Authorization Design

- **Coarse-grained:** gateway checks role claims for route groups (`/admin/**` requires `ADMIN` or `SUPER_ADMIN`).
- **Fine-grained:** method security in each service — `@PreAuthorize("hasAuthority('COURSE_PUBLISH')")`.
- **Ownership:** resource-level checks in the service layer (`course.instructorId == principal.id || hasRole('ADMIN')`). Never rely on client-supplied owner IDs.
- **Enrolment gate:** every lesson/media/assessment read verifies an `ACTIVE` enrolment (cached 60 s in Redis).
- **Deny by default:** any endpoint without an explicit rule requires authentication.

## 9.3 Threat Model and Controls (OWASP Top 10 mapping)

| Threat | Control |
|---|---|
| Broken access control | Deny-by-default, ownership checks, integration tests per role, no IDOR-exposed sequential IDs (UUIDs) |
| Cryptographic failures | TLS 1.2+ everywhere, secrets in a secret manager, 2FA secrets and tokens encrypted/hashed at rest, RDS storage encryption |
| Injection | Parameterized JPA/JDBC only; no string-concatenated SQL; Bean Validation on all inputs; output encoding in React |
| Insecure design | Threat modelling per module, rate limits on all auth flows, server-side price and timer authority |
| Security misconfiguration | Hardened container images (distroless/alpine, non-root), Swagger and actuator endpoints disabled/secured in prod, CSP + HSTS + `X-Content-Type-Options` + `Referrer-Policy` headers |
| Vulnerable components | Dependabot/Renovate, OWASP Dependency-Check and Trivy image scanning in CI, build fails on High/Critical |
| Auth failures | Lockout, throttling, credential-stuffing detection, breached-password check (k-anonymity API), generic error messages |
| Data integrity failures | Signed webhooks, checksum on uploads, signed container images, protected branches with review |
| Logging failures | Structured JSON logs, correlation IDs, security events to the audit log, alerting on anomalies; secrets and tokens redacted |
| SSRF | No user-supplied URL fetching; egress allow-list from the cluster |
| XSS | React auto-escaping, sanitize any HTML content (course descriptions) with an allow-list sanitizer, CSP without `unsafe-inline` |
| CSRF | Bearer-token APIs are not cookie-authenticated; the refresh cookie uses `SameSite=Strict` plus a double-submit token on `/auth/refresh` |
| File upload abuse | Presigned URLs scoped to key prefix + content-type + size, MIME sniffing, extension allow-list, malware scan hook, no execution from the media bucket |

## 9.4 Rate Limits (defaults)

| Endpoint group | Limit |
|---|---|
| `POST /auth/login` | 10 per 15 min per IP + 5 per account |
| `POST /auth/register` | 5 per hour per IP |
| `POST /auth/otp/send` | 5 per hour per email, 60 s cooldown |
| `POST /auth/forgot-password` | 5 per hour per email |
| Authenticated general API | 300 per minute per user |
| Anonymous catalogue | 60 per minute per IP |
| Chat send (WebSocket) | 10 per 10 s per user |
| Upload URL requests | 30 per hour per instructor |

## 9.5 Compliance and Privacy

- **PCI DSS:** gateway-hosted checkout only → SAQ-A scope; no PAN ever touches our systems.
- **GDPR / India DPDP:** consent capture at registration, data export endpoint, right to erasure via anonymization, documented retention (§6.12), DPA with sub-processors.
- **Accessibility:** WCAG 2.1 AA.
- **Terms:** learner content ownership and instructor revenue share captured in signed agreements referenced by the platform.

---

# 10. Real-Time Subsystem Design

## 10.1 Connection Lifecycle

1. Client opens `wss://api.example.com/ws` with the access token as a `Sec-WebSocket-Protocol` value or a short-lived ticket query parameter (tickets avoid tokens in URLs and logs).
2. A `HandshakeInterceptor` validates the token, resolves the principal and attaches it to the session.
3. Client sends `CONNECT`, then `SUBSCRIBE`s to its rooms and `/user/queue/**`.
4. Heartbeats every 25 s in both directions; a missed heartbeat closes the socket and the client reconnects with backoff and jitter.
5. On reconnect the client calls `GET /chats/{id}/messages?after={lastSeenUlid}` to backfill anything missed.

## 10.2 Multi-Instance Fan-Out

With N replicas of `chat-service`, a message published on one node must reach subscribers on all nodes:

```
client A ─► node1 ─► persist message ─► Redis PUBLISH chat.{chatId} ─┬─► node1 → local subscribers
                                                                     ├─► node2 → local subscribers
                                                                     └─► node3 → local subscribers
```

Options: Redis pub/sub relay (chosen for v1 — simple, low latency) or a full STOMP broker relay to RabbitMQ (`enableStompBrokerRelay`) when durability and scale demand it.

## 10.3 Message Delivery Semantics

| Concern | Approach |
|---|---|
| Ordering | ULID message IDs are monotonic per chat; clients sort by ID, not by arrival |
| Optimistic UI | Client sends `tempId`; the server echo carries both `tempId` and the real ID so the client reconciles |
| Duplicates | Server deduplicates on `(senderId, tempId)` within a 60-second window |
| Offline users | Message persisted; `notification.created` event drives an in-app/email notification |
| Typing | Ephemeral Redis hash with a 5-second TTL; never persisted |
| Read receipts | Client posts `lastReadMessageId`; server updates the member row, recomputes unread count and pushes receipts to other members |
| Presence | `SETEX chat:presence:{userId} 60` heartbeat; absence implies offline |

## 10.4 Real-Time Notifications

`notification-service` consumes domain events, writes the notification row, then pushes to `/user/{userId}/queue/notifications` if the user has a live session (tracked in `ws:session:{userId}`); otherwise the badge count updates on next fetch.

---

# 11. Non-Functional Requirements

## 11.1 Performance

| ID | Requirement |
|---|---|
| NFR-PERF-01 | 95th-percentile API latency ≤ 300 ms for reads and ≤ 500 ms for writes, excluding third-party calls, at target load. |
| NFR-PERF-02 | Catalogue search shall return within 400 ms for a 10,000-course dataset. |
| NFR-PERF-03 | Chat message end-to-end delivery ≤ 500 ms at the 95th percentile within a region. |
| NFR-PERF-04 | Video start (first frame) ≤ 2 s on a 10 Mbps connection via CDN. |
| NFR-PERF-05 | The system shall sustain 1,000 concurrent active users and 200 concurrent streams on the baseline deployment. |
| NFR-PERF-06 | No endpoint shall issue N+1 queries; batch fetching or join fetches are mandatory for collection reads. |

## 11.2 Scalability

| ID | Requirement |
|---|---|
| NFR-SCAL-01 | All services shall be stateless and horizontally scalable; HPA targets 70 % CPU / 75 % memory, 2–10 replicas. |
| NFR-SCAL-02 | PostgreSQL shall support read replicas for analytics and catalogue reads. |
| NFR-SCAL-03 | Chat and media shall scale independently of the core API. |
| NFR-SCAL-04 | Large tables (messages, audit_logs, progress) shall be partitioned by month once they exceed 50 M rows. |

## 11.3 Availability & Reliability

| ID | Requirement |
|---|---|
| NFR-AVL-01 | Monthly availability target 99.5 % for v1 (99.9 % post-GA). |
| NFR-AVL-02 | Minimum 2 replicas per service; rolling updates with zero downtime; readiness and liveness probes on every pod. |
| NFR-AVL-03 | RPO ≤ 15 minutes (PITR), RTO ≤ 1 hour; daily full backups retained 30 days; restore tested quarterly. |
| NFR-AVL-04 | Degradation of Redis, RabbitMQ or the transcoder shall not make core learning flows unavailable (see §12.3). |

## 11.4 Security, Maintainability, Usability, Portability

| ID | Requirement |
|---|---|
| NFR-SEC-01 | All controls in Section 9 shall be implemented and verified by automated tests where feasible. |
| NFR-MNT-01 | Unit test line coverage ≥ 80 % on service layers; the build fails below the threshold. |
| NFR-MNT-02 | Static analysis (SonarQube/SpotBugs, ESLint, Checkstyle) shall pass with zero blocker/critical issues. |
| NFR-MNT-03 | Every service ships a README, OpenAPI document and runnable local profile. |
| NFR-USB-01 | Core flows (register → enrol → learn) shall be completable in ≤ 5 interactions from the landing page. |
| NFR-USB-02 | All error messages shall be human-readable and actionable; no stack traces reach the client. |
| NFR-PRT-01 | No cloud-proprietary API beyond S3-compatible storage and managed Postgres; deployment shall be reproducible on any Kubernetes cluster. |
| NFR-OBS-01 | Every service shall expose `/actuator/health`, `/actuator/prometheus`, and emit structured JSON logs with correlation IDs. |

---

# 12. Error Handling, Resilience and Degraded Modes

## 12.1 Exception Strategy

A shared `common-lib` provides `BusinessException(code, message, status)`, `ResourceNotFoundException`, `ForbiddenOperationException` and a `@RestControllerAdvice` global handler that maps exceptions to the standard envelope, logs at the correct level (4xx = WARN, 5xx = ERROR with stack trace) and never leaks internals.

## 12.2 Resilience Patterns

| Pattern | Applied to | Configuration |
|---|---|---|
| Timeout | All outbound HTTP | connect 2 s, read 3 s |
| Retry | Idempotent GETs, broker publishes, S3 | 3 attempts, exponential backoff + jitter |
| Circuit breaker | Cross-service calls, gateway integrations | 50 % failure rate over 20 calls → open 30 s |
| Bulkhead | Media and analytics calls | separate thread pools / connection limits |
| Fallback | Course detail enrichment | serve cached or partial data with a `degraded: true` flag |
| Outbox | Event publication | write event row in the same transaction; a relay publishes and marks it sent — no lost events |
| DLQ | Consumers | 3 redeliveries → DLQ + alert; manual replay tool |

## 12.3 Degraded Mode Matrix

| Failed dependency | Impact | Behaviour |
|---|---|---|
| Redis | Cache and rate limits | Bypass cache, fall back to fixed in-memory rate limits, log warning; core flows continue |
| RabbitMQ/Kafka | Notifications, auto-enrolment | Events buffered in the outbox; a reconciliation job drains them on recovery; payments still succeed |
| Transcoder | New video processing | Lesson stays `PROCESSING`; existing content unaffected; instructor notified |
| Payment gateway | Checkout | Checkout disabled with a clear message; free enrolments continue |
| SMTP | Email | Queued and retried; in-app notifications still delivered |
| Chat service | Chat only | Chat UI shows offline banner; learning and assessment unaffected |

---

# 13. DevOps: Containerization and CI/CD

## 13.1 Container Strategy

Multi-stage builds; final images run as a non-root user with a read-only filesystem where possible.

```dockerfile
# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

Frontend image: Node build stage → `nginx:alpine` runtime with gzip/brotli, SPA fallback (`try_files $uri /index.html`), long-lived cache headers on hashed assets and `no-cache` on `index.html`.

## 13.2 Local Orchestration (`docker-compose.yml` — services)

| Service | Image | Ports | Notes |
|---|---|---|---|
| postgres | postgres:16-alpine | 5432 | one DB per service via init script |
| redis | redis:7-alpine | 6379 | `--appendonly yes` |
| rabbitmq | rabbitmq:3.13-management | 5672, 15672 | management UI |
| minio | minio/minio | 9000, 9001 | S3-compatible local storage |
| mailhog | mailhog/mailhog | 1025, 8025 | captures outbound mail |
| api-gateway … payment-service | locally built | 8080–8087 | `depends_on` with health conditions |
| frontend | locally built | 3000 | proxies `/api` to the gateway |
| prometheus / grafana | official | 9090 / 3001 | optional observability profile |

## 13.3 CI/CD Pipeline (GitHub Actions)

Pipeline as specified in the source document: **GitHub → GitHub Actions → Build & Test → Docker Image → Docker Hub → Kubernetes → Cloud**.

| Stage | Action | Gate |
|---|---|---|
| 1. Checkout & cache | actions/checkout, setup-java 21, Maven cache | — |
| 2. Lint & static analysis | Checkstyle, SpotBugs, ESLint, `tsc --noEmit` | Zero blockers |
| 3. Unit tests | `mvn test`, `vitest run` | Coverage ≥ 80 % (JaCoCo) |
| 4. Integration tests | Testcontainers (Postgres, Redis, RabbitMQ) | All green |
| 5. Security scan | OWASP Dependency-Check, `npm audit`, Trivy on the built image, Gitleaks | No High/Critical |
| 6. Build | `mvn package -DskipTests`, `npm run build` | Artifact produced |
| 7. Image | Buildx multi-arch, tag `sha-<short>` and `v<semver>` | — |
| 8. Publish | Push to Docker Hub / GHCR | Only on `develop`, `main`, tags |
| 9. Deploy dev | `kubectl set image` or Helm upgrade to the dev namespace | Auto |
| 10. Smoke tests | Health checks + a critical-path Postman/Newman run | Auto rollback on failure |
| 11. Deploy staging | Helm upgrade with the staging values file | On `release/*` |
| 12. Deploy prod | Helm upgrade, rolling strategy | Manual approval on tag |

Branching: trunk-friendly Git Flow — `feature/*` → `develop` → `release/*` → `main`, with `hotfix/*` off `main`. Conventional Commits drive semantic versioning and changelog generation. `main` and `develop` are protected: PR review + green CI required.

## 13.4 Kubernetes Objects (per service)

`Deployment` (2+ replicas, rolling update `maxUnavailable: 0`, resource requests/limits, liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`), `Service` (ClusterIP), `ConfigMap` (non-secret config), `Secret` (from AWS Secrets Manager via External Secrets Operator), `HorizontalPodAutoscaler`, `PodDisruptionBudget (minAvailable: 1)`, `NetworkPolicy` (only the gateway may reach service ports), and one shared `Ingress` (NGINX or ALB) terminating TLS via cert-manager.

## 13.5 Observability

| Concern | Tooling | Key signals |
|---|---|---|
| Metrics | Micrometer → Prometheus → Grafana | request rate, error rate, p95 latency, JVM heap/GC, DB pool usage, queue depth, WS connections |
| Logs | Logback JSON → Fluent Bit → Elasticsearch/Loki → Kibana/Grafana | correlation ID, user ID, service, level |
| Traces | Micrometer Tracing + OpenTelemetry → Jaeger/Tempo | cross-service spans for slow requests |
| Alerts | Alertmanager → Slack/email | 5xx > 1 % for 5 min, p95 > 1 s, pod crash-loop, DLQ depth > 0, disk > 80 %, cert expiry < 14 days |
| Uptime | External synthetic checks | login, catalogue, playback URL issuance |

---

# 14. Deployment and Infrastructure

## 14.1 AWS Reference Topology

| Layer | Service | Configuration |
|---|---|---|
| DNS & CDN | Route 53 + CloudFront | Static SPA and media origins, ACM certificates |
| Ingress | ALB → NGINX Ingress on EKS | TLS termination, WAF rules |
| Compute | EKS (managed node group, 3 × t3.large across 3 AZs) | Cluster autoscaler enabled |
| Database | RDS PostgreSQL 16, Multi-AZ, `db.t3.medium` | Automated backups, PITR, encrypted |
| Cache | ElastiCache Redis 7, replication group | Encryption in transit and at rest |
| Broker | Amazon MQ (RabbitMQ) or MSK | Private subnets |
| Storage | S3 buckets: `lms-media-raw`, `lms-media-hls`, `lms-static`, `lms-invoices` | Versioning, lifecycle rules, block public access; delivery only via CloudFront OAC |
| Email | SES with a verified domain | SPF, DKIM, DMARC |
| Secrets | Secrets Manager + External Secrets Operator | Rotation enabled |
| Network | VPC, public subnets (ALB/NAT), private subnets (nodes, RDS, cache) | Security groups least-privilege |

## 14.2 Environment Configuration (`.env` / ConfigMap keys)

```
SPRING_PROFILES_ACTIVE=prod
DB_URL / DB_USERNAME / DB_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
RABBIT_HOST / RABBIT_USERNAME / RABBIT_PASSWORD
JWT_PRIVATE_KEY / JWT_PUBLIC_KEY / JWT_ACCESS_TTL=900 / JWT_REFRESH_TTL=604800
GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET / OAUTH_REDIRECT_URI
MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM
AWS_REGION / S3_BUCKET_MEDIA / S3_BUCKET_STATIC / CLOUDFRONT_DOMAIN / CLOUDFRONT_KEY_PAIR_ID
PAYMENT_GATEWAY=RAZORPAY / PAYMENT_KEY_ID / PAYMENT_KEY_SECRET / PAYMENT_WEBHOOK_SECRET
APP_BASE_URL / FRONTEND_URL / CORS_ALLOWED_ORIGINS
OTP_TTL_MINUTES=10 / OTP_MAX_ATTEMPTS=5
RATE_LIMIT_ENABLED=true
```

## 14.3 Release Checklist

1. All migrations are backward-compatible (expand → migrate → contract) so old and new pods can run together.
2. Feature flags default to off for incomplete features.
3. Staging smoke suite green; performance run within budget.
4. Backup verified within the last 24 hours; rollback image tag identified.
5. Runbook and on-call owner confirmed; change logged.
6. Post-deploy: watch error rate and latency for 30 minutes; roll back on breach.

---

# 15. Step-by-Step Implementation Guide

This is the executable build order. Each step lists its goal, tasks and a **Done when** condition. Estimated effort assumes one full-stack developer; parallelize across a team by phase.

## Phase 0 — Foundation (Week 1)

**Step 1 — Tooling and accounts.** Install JDK 21, Maven 3.9, Node 20, Docker Desktop, Git, IntelliJ/VS Code, Postman, DBeaver. Create GitHub organization and repository, Docker Hub account, AWS free-tier account, Google Cloud OAuth credentials, payment gateway test account.
*Done when:* `java -version`, `mvn -v`, `node -v`, `docker run hello-world` all succeed.

**Step 2 — Repository skeleton.** Choose a monorepo:
```
enterprise-lms/
├── backend/{api-gateway,auth-service,user-service,course-service,media-service,chat-service,notification-service,payment-service,common-lib}
├── frontend/
├── infra/{docker,k8s,helm,terraform}
├── docs/{srs.md,api,adr}
└── .github/workflows/
```
Add `.gitignore`, `README.md`, `LICENSE`, `CODEOWNERS`, PR template, Conventional Commit lint, EditorConfig.
*Done when:* the skeleton is pushed and branch protection is on.

**Step 3 — Local infrastructure.** Write `infra/docker/docker-compose.infra.yml` for Postgres, Redis, RabbitMQ, MinIO, MailHog. Create databases `lms_auth`, `lms_user`, `lms_course`, `lms_media`, `lms_chat`, `lms_notify`, `lms_pay` via an init script.
*Done when:* `docker compose up -d` is healthy and DBeaver connects.

**Step 4 — `common-lib`.** Shared module: `ApiResponse<T>`, `ErrorCode` enum, exceptions, `GlobalExceptionHandler`, `BaseEntity` (auditing + `@Version`), `CorrelationIdFilter`, `JwtClaims`, `PageResponse<T>`, Jackson config (UTC, snake/camel policy), `SecurityUtils.getCurrentUserId()`.
*Done when:* `mvn install` publishes it locally and another module can import it.

## Phase 1 — Authentication Core (Weeks 2–3)

**Step 5 — Bootstrap `auth-service`.** Spring Boot 3 with Web, Security, Data JPA, Validation, Redis, Mail, Flyway, Actuator, springdoc. Configure `application.yml` profiles (`local`, `dev`, `prod`).
*Done when:* the service starts and `/actuator/health` returns `UP`.

**Step 6 — Baseline migration.** `V1__auth_baseline.sql` creating `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `otps`, `oauth_accounts`, `audit_logs`. Seed the four roles and a Super Admin.
*Done when:* Flyway applies cleanly on a fresh database.

**Step 7 — Register + email verification.** `RegisterRequest` with Bean Validation, BCrypt hashing, duplicate-email check, OTP generation and hashing, mail dispatch (MailHog locally), `/auth/verify-email` and `/auth/otp/verify`.
*Done when:* a new user registers, receives an OTP in MailHog, verifies, and becomes `ACTIVE`.

**Step 8 — Login + JWT.** RSA key pair, `JwtService` (generate/parse/validate), `AuthenticationManager` wiring, `JwtAuthenticationFilter`, `SecurityFilterChain` (stateless, CORS, public/private matchers), failed-attempt lockout.
*Done when:* login returns tokens and a protected endpoint accepts them and rejects tampered ones.

**Step 9 — Refresh rotation and logout.** Store token hashes with `family_id`, rotate on refresh, detect reuse and revoke the family, blacklist `jti` on logout, `logout-all`, session listing.
*Done when:* replaying an old refresh token returns `401 TOKEN_REUSE_DETECTED` and kills all sessions.

**Step 10 — RBAC.** Load authorities from roles + permissions, enable `@EnableMethodSecurity`, add `@PreAuthorize` on admin endpoints, cache the permission set in Redis with invalidation on role change.
*Done when:* a Student receives `403` on `/admin/users` and an Admin receives `200`.

**Step 11 — Password reset, 2FA, OAuth2.** Reset token flow; TOTP enrolment with QR (`otpauth://`), verification, recovery codes; Google OAuth2 login with PKCE, account linking and auto-provisioning.
*Done when:* all three flows pass manual and integration tests.

**Step 12 — Auth hardening.** Rate limiting (Bucket4j + Redis) on auth routes, audit logging on every auth event, generic error messages, `springdoc` documentation.
*Done when:* the 11th login attempt in 15 minutes returns `429` with `Retry-After`.

## Phase 2 — Gateway and User Service (Week 4)

**Step 13 — API Gateway.** Spring Cloud Gateway: routes to each service, global JWT validation filter, header injection (`X-User-Id`, `X-Roles`, `X-Correlation-Id`), CORS, rate-limit filter, Resilience4j circuit breakers, aggregated Swagger.
*Done when:* all client traffic works through `:8080` and direct service ports can be firewalled.

**Step 14 — User service.** Profiles, avatar upload hook, preferences, instructor application workflow, admin user search; consumes `user.registered` to create the profile row.
*Done when:* registering a user automatically creates a profile via the event.

## Phase 3 — Course Domain (Weeks 5–7)

**Step 15 — Course CRUD and lifecycle.** Entities and migrations for categories, courses, modules, lessons; slug generation; ownership checks; state machine with validation on publish; admin approve/reject.
*Done when:* an instructor can build a course, submit it, and an admin can approve and publish it.

**Step 16 — Catalogue and search.** Specification/Criteria-based dynamic filters, full-text search on `tsvector`, sorting, pagination, Redis caching with invalidation on publish/update.
*Done when:* `/courses?q=java&level=BEGINNER&sort=rating,desc` returns correct, cached results within budget.

**Step 17 — Enrolment and progress.** Free enrolment, duplicate guard, `lesson_progress` upsert, throttled position writes, completion percentage recomputation, `course.completed` event.
*Done when:* completing all lessons sets the enrolment to `COMPLETED` and emits the event.

**Step 18 — Reviews.** One review per enrolled student, aggregate rating recomputation, moderation flag.
*Done when:* the course rating updates atomically on review create/update/delete.

## Phase 4 — Media and Playback (Week 8)

**Step 19 — Media service and upload.** Presigned multipart upload URLs, completion endpoint, MIME sniffing, size limits, checksum, media status machine.
*Done when:* a 500 MB video uploads directly to MinIO/S3 from the browser with progress reporting.

**Step 20 — Transcoding and streaming.** Queue `media.uploaded`, FFmpeg worker producing HLS renditions + thumbnails, publish `media.ready`, `course-service` attaches the manifest to the lesson; signed playback URLs gated by enrolment.
*Done when:* an uploaded video plays as adaptive HLS and a non-enrolled user receives `403`.

**Step 21 — Player integration.** hls.js player with quality/speed controls, resume prompt, throttled progress beacons, auto-complete at 90 %.
*Done when:* closing and reopening a lesson resumes within ±2 s of the last position.

## Phase 5 — Assessment (Weeks 9–10)

**Step 22 — Assignments.** CRUD, submission with files, late handling, versioning, grading endpoint, `assignment.graded` event, instructor grading queue.
*Done when:* a graded submission notifies the student and shows the score.

**Step 23 — Quizzes.** Quiz/question/option CRUD, attempt start with server-side timer in Redis, answer autosave, submit with auto-evaluation, negative marking, attempt limits, results with the show-answers policy.
*Done when:* correct answers never appear in attempt payloads and an expired attempt auto-submits.

## Phase 6 — Real-Time and Notifications (Weeks 11–12)

**Step 24 — Chat service.** WebSocket/STOMP configuration, handshake authentication, room provisioning on `enrolment.created`, send/persist/broadcast, history with cursor pagination, edit/delete, moderation.
*Done when:* two browsers exchange messages in real time and history reloads correctly.

**Step 25 — Typing, receipts, presence, scaling.** Typing indicator with TTL, read receipts and unread counts, presence heartbeat, Redis pub/sub fan-out across replicas, per-user send rate limit.
*Done when:* messages are delivered correctly with two `chat-service` replicas running.

**Step 26 — Notification service.** Event consumers with an idempotency table, notification persistence, WebSocket push, email templates, preferences, DLQ handling.
*Done when:* enrolment, grading and payment events each produce exactly one notification, even on duplicate delivery.

## Phase 7 — Commerce and Certificates (Weeks 13–14)

**Step 27 — Payments.** Order creation with server-side pricing, coupon application, gateway order creation, checkout UI, webhook endpoint with signature verification and idempotency, transaction ledger, `payment.succeeded` → auto-enrolment, order expiry job, invoice PDF, admin refunds.
*Done when:* a test-mode purchase enrols the student only after the verified webhook, and replaying the webhook changes nothing.

**Step 28 — Certificates.** Listener on `course.completed`, verification code generation, HTML→PDF rendering, S3 upload, download and public verification endpoints, revocation.
*Done when:* completing a course produces a downloadable PDF whose code verifies publicly.

## Phase 8 — Analytics and Admin (Week 15)

**Step 29 — Analytics.** Event/telemetry capture, nightly rollup jobs (ShedLock) into summary tables, instructor and admin dashboard endpoints, CSV export, Recharts dashboards.
*Done when:* dashboards render from summary tables with no heavy live aggregation.

**Step 30 — Admin panel.** User management, moderation queue, payment reconciliation, audit log viewer with filters, feature flags, category management.
*Done when:* every admin action appears in the audit log with before/after snapshots.

## Phase 9 — Hardening, Testing, Delivery (Weeks 16–18)

**Step 31 — Test suite completion.** Unit tests to ≥ 80 % on service layers, Testcontainers integration tests per service, contract tests on the gateway, Playwright/Cypress E2E for the five critical journeys, k6/JMeter load test at target concurrency.
*Done when:* CI runs the full suite green and load targets in §11.1 are met.

**Step 32 — Security review.** OWASP ASVS checklist walkthrough, dependency and image scans clean, penetration-style manual tests (IDOR, privilege escalation, token replay, upload abuse), secrets audit.
*Done when:* no High/Critical findings remain open.

**Step 33 — Observability and runbooks.** Prometheus scrape configs, Grafana dashboards, alert rules, log shipping, tracing, on-call runbooks for the top 10 failure modes.
*Done when:* a simulated database outage triggers an alert and the runbook resolves it.

**Step 34 — Kubernetes and CI/CD to staging.** Helm charts per service, values per environment, secrets via External Secrets, ingress + TLS, HPA, PDB, full pipeline through to staging deploy plus smoke tests.
*Done when:* a merge to `develop` reaches dev automatically and `release/*` reaches staging.

**Step 35 — Production launch.** Terraform the AWS baseline, restore-tested backups, DNS and certificates, WAF rules, blue-green or canary rollout of `v1.0.0`, 48-hour hypercare with dashboards.
*Done when:* production serves real traffic within SLOs and rollback has been rehearsed.

**Step 36 — Documentation and handover.** Publish this SRS, ADRs for key decisions (microservices split, token strategy, media pipeline), Postman collection, README quick-start (`docker compose up` → seeded demo data), architecture diagram, demo script and interview talking points.
*Done when:* a new developer runs the whole stack locally in under 30 minutes using only the docs.

## 15.1 Suggested Timeline

| Phase | Weeks | Deliverable |
|---|---|---|
| 0 Foundation | 1 | Repo, infra, common library |
| 1 Auth core | 2–3 | Complete identity subsystem |
| 2 Gateway & user | 4 | Unified entry point, profiles |
| 3 Course domain | 5–7 | Authoring, catalogue, enrolment, progress |
| 4 Media | 8 | Upload, transcode, secure streaming |
| 5 Assessment | 9–10 | Assignments and quizzes |
| 6 Real-time | 11–12 | Chat and notifications |
| 7 Commerce | 13–14 | Payments and certificates |
| 8 Analytics & admin | 15 | Dashboards and admin console |
| 9 Hardening & launch | 16–18 | Tests, security, K8s, production |

## 15.2 Minimum Viable Demo Path

If time is short, build Steps 1–10, 13, 15–17, 19–21, 23 and 24. That yields a demonstrable product: register → verify → login (JWT + refresh) → browse → enrol → watch with resume → take a quiz → chat in real time — which already evidences every headline skill in the objectives (React, Tailwind, Java, Spring Boot, Hibernate, JDBC, PostgreSQL, JWT, OAuth2, WebSocket, Docker).

---

# 16. Repository Structure and Engineering Standards

## 16.1 Service Package Layout

```
com.lms.<service>
├── config/          SecurityConfig, RedisConfig, RabbitConfig, OpenApiConfig, WebSocketConfig
├── controller/      thin REST layer, validation only
├── dto/             request/ and response/ records
├── entity/          JPA entities extending BaseEntity
├── repository/      Spring Data JPA + custom JDBC where performance demands
├── service/         interfaces + impl — all business rules live here
├── mapper/          MapStruct mappers
├── event/           publishers, listeners, payload records
├── exception/       service-specific exceptions
├── security/        filters, principal, permission evaluators
└── util/            helpers, constants
```

## 16.2 Coding Standards

- Java: Google Java Format, Lombok limited to `@Getter/@Setter/@Builder/@RequiredArgsConstructor`, constructor injection only, no field injection, entities never returned from controllers (DTOs only), `@Transactional` on service methods with explicit `readOnly = true` for queries.
- Naming: `CourseService.publishCourse()`, DTOs `CreateCourseRequest` / `CourseDetailResponse`, tables and columns `snake_case`, endpoints plural nouns.
- TypeScript: strict mode, no `any`, DTO types generated from OpenAPI, components ≤ 200 lines, custom hooks for logic reuse.
- Every PR: description, linked issue, screenshots for UI changes, tests, migration reviewed, no commented-out code.
- ADRs (`docs/adr/NNN-title.md`) for every architectural decision.

---

# 17. Testing Strategy

| Level | Scope | Tools | Target |
|---|---|---|---|
| Unit | Service/business logic, validators, mappers, utilities | JUnit 5, Mockito, AssertJ | ≥ 80 % line coverage |
| Slice | Controllers, repositories, security rules | `@WebMvcTest`, `@DataJpaTest`, Spring Security Test | All endpoints |
| Integration | Service + real Postgres/Redis/RabbitMQ | Testcontainers | Every critical flow |
| Contract | Gateway ↔ services, event payloads | Spring Cloud Contract / Pact | All published events |
| End-to-end | Browser journeys | Playwright or Cypress | 5 critical paths |
| Performance | Load, soak, spike | k6 / JMeter / Gatling | Meets §11.1 |
| Security | Dependency, image, secrets, ZAP baseline | Dependency-Check, Trivy, Gitleaks, OWASP ZAP | No High/Critical |
| Accessibility | Automated + manual | axe-core, keyboard walkthrough | WCAG 2.1 AA |

**Five critical E2E journeys:** (1) register → verify → login; (2) instructor authors and publishes a course with video; (3) student purchases and enrols; (4) student watches with resume, takes a quiz, submits an assignment; (5) completion → certificate → public verification.

**Test data:** Flyway `afterMigrate` seed for local/dev — 3 categories, 5 courses, 2 instructors, 10 students, sample quiz and assignment, one completed enrolment.

---

# 18. Future Enhancements (Post-v1 Roadmap)

| # | Enhancement | Notes |
|---|---|---|
| 1 | **AI course assistant** | RAG over lesson transcripts and resources; per-course scoped answers with citations to timestamps |
| 2 | **Recommendation engine** | Collaborative filtering on enrolment/completion signals plus content-based tag similarity; served from a batch-computed table |
| 3 | **Live classes** | WebRTC SFU (LiveKit/Janus) with scheduling, recording to S3, attendance capture |
| 4 | **Mobile apps** | React Native or Flutter clients reusing the same API; offline lesson download with encrypted local storage |
| 5 | **Multi-language support** | i18next on the front end, `Accept-Language`-aware message bundles, translated course metadata |
| 6 | **Gamification** | Points, badges, streaks, leaderboards |
| 7 | **Multi-tenancy / white-label** | Tenant column + schema isolation, custom domains, per-tenant theming |
| 8 | **Advanced proctoring** | Webcam snapshots, tab-switch detection, plagiarism checks |
| 9 | **SCORM/xAPI** | Import third-party packages, emit xAPI statements to an LRS |
| 10 | **Instructor payouts** | Automated split settlements, tax documents, payout schedules |

---

# 19. Acceptance Criteria and Traceability

## 19.1 Definition of Done (per feature)

1. Requirement implemented and matching its acceptance criteria.
2. Unit + integration tests written and passing; coverage threshold held.
3. OpenAPI updated; Postman collection updated.
4. Security review of the endpoint (authz rule, validation, rate limit).
5. Migration reviewed and backward-compatible.
6. Logging, metrics and audit entries added where relevant.
7. UI states covered: loading, empty, error, success; responsive at all breakpoints.
8. PR reviewed and approved; CI green; documentation updated.

## 19.2 Release Acceptance (v1.0)

| Criterion | Gate |
|---|---|
| All P0 requirements implemented | 100 % |
| P1 requirements | ≥ 80 % |
| Critical/High defects open | 0 |
| Coverage | ≥ 80 % service layer |
| Load test at target concurrency | Meets §11.1 |
| Security scan | No High/Critical |
| Backup restore drill | Passed |
| E2E journeys | All 5 green |
| Documentation | SRS, API docs, runbooks published |

## 19.3 Traceability Matrix (extract)

| Source objective | Requirements | Build steps |
|---|---|---|
| Showcase React, Tailwind, Java, Spring Boot, Hibernate, JDBC, PostgreSQL | §8, §6, all services | Steps 4–30 |
| Authentication, authorization, email OTP, JWT, OAuth2 | FR-AUTH-01…18, §9 | Steps 5–12 |
| Microservices, Docker, Kubernetes, CI/CD, cloud | §3, §13, §14 | Steps 2–3, 13, 34–35 |
| Scalable architecture and real-time features | §10, §11.2, FR-CHT-* | Steps 24–26, 31 |
| Core modules (10) | §4.1–§4.12 | Steps 5–30 |
| Advanced features (resume playback, typing, receipts, search, caching, audit, monitoring, REST, upload, responsive UI) | FR-ENR-04, FR-CHT-05/06, FR-CRS-08, §6.11, FR-ADM-04, §13.5, §7, FR-MED-01, §8 | Steps 16–33 |
| Deployment pipeline | §13.3 | Steps 34–35 |

---

# 20. Appendices

## Appendix A — Domain Event Catalogue

| Event | Publisher | Payload (key fields) | Consumers |
|---|---|---|---|
| `user.registered` | auth | userId, email, fullName | user, notification |
| `user.verified` | auth | userId | notification |
| `course.published` | course | courseId, instructorId, title, price | payment, notification, search index |
| `enrolment.created` | course | enrolmentId, courseId, studentId | chat (room join), notification, analytics |
| `progress.updated` | course | enrolmentId, lessonId, percent | analytics |
| `course.completed` | course | enrolmentId, courseId, studentId | certificate issuer, notification |
| `assignment.graded` | course | submissionId, studentId, score | notification |
| `quiz.submitted` | course | attemptId, studentId, passed | notification, analytics |
| `media.uploaded` | media | mediaId, storageKey | transcoder |
| `media.ready` | media | mediaId, hlsUrl, duration | course, notification |
| `chat.message.created` | chat | messageId, chatId, senderId | notification |
| `payment.succeeded` | payment | orderId, userId, courseId, amount | course (enrol), notification, analytics |
| `payment.failed` | payment | orderId, reason | notification |
| `refund.completed` | payment | refundId, orderId | course (revoke), notification |

Envelope: `{ eventId, eventType, occurredAt, version, correlationId, payload }`. Routing key convention: `<domain>.<entity>.<action>` on the topic exchange `lms.events`.

## Appendix B — Sample Entity (JPA)

```java
@Entity
@Table(name = "courses",
       indexes = { @Index(name = "idx_courses_status", columnList = "status, published_at") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course extends BaseEntity {

    @Column(nullable = false)          private UUID instructorId;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(columnDefinition = "text", nullable = false)   private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "price_minor", nullable = false) private Long priceMinor = 0L;
    @Column(length = 3) private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CourseModule> modules = new ArrayList<>();

    public void publish() {
        if (modules.isEmpty()) throw new BusinessException(ErrorCode.INSUFFICIENT_COURSE_CONTENT);
        this.status = CourseStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}
```

## Appendix C — Sample JDBC Query (reporting path)

```java
private static final String TOP_COURSES = """
    SELECT c.id, c.title, COUNT(e.id) AS enrolments,
           COALESCE(SUM(o.total_minor), 0) AS revenue_minor
    FROM courses c
    LEFT JOIN enrolments e ON e.course_id = c.id AND e.enrolled_at >= ?
    LEFT JOIN orders o     ON o.course_id = c.id AND o.status = 'PAID'
    WHERE c.status = 'PUBLISHED'
    GROUP BY c.id, c.title
    ORDER BY enrolments DESC
    LIMIT ?
    """;

public List<TopCourseRow> topCourses(Instant since, int limit) {
    return jdbcTemplate.query(TOP_COURSES, topCourseMapper, Timestamp.from(since), limit);
}
```

Hibernate/JPA is used for transactional aggregates; plain JDBC (`JdbcTemplate`) is used for reporting and bulk paths where control over the SQL plan matters — satisfying the objective of demonstrating both.

## Appendix D — Glossary of Status Enums

| Entity | Statuses |
|---|---|
| User | PENDING_VERIFICATION, ACTIVE, SUSPENDED, DELETED |
| Course | DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, ARCHIVED |
| Enrolment | ACTIVE, COMPLETED, CANCELLED, REFUNDED |
| LessonProgress | NOT_STARTED, IN_PROGRESS, COMPLETED |
| Media | PENDING, UPLOADED, PROCESSING, READY, FAILED |
| Submission | SUBMITTED, UNDER_REVIEW, GRADED, RETURNED |
| QuizAttempt | IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED, EXPIRED |
| Order | CREATED, PENDING, PAID, FAILED, EXPIRED, REFUNDED |
| Payment | AUTHORIZED, CAPTURED, FAILED, REFUNDED |
| Certificate | ISSUED, REVOKED |

## Appendix E — Risk Register

| # | Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|---|
| R1 | Scope creep across 10 modules | Schedule slip | High | Fixed P0 scope, phase gates, MVP path in §15.2 |
| R2 | Transcoding cost/complexity | Budget, delay | Medium | Start with single-rendition MP4; add HLS ladder later |
| R3 | Distributed data consistency (payment → enrolment) | Paid but not enrolled | Medium | Outbox + idempotent consumers + reconciliation job + alert |
| R4 | WebSocket scaling across replicas | Missed messages | Medium | Redis relay from day one; sticky sessions at the ingress |
| R5 | Cloud cost overrun | Budget | Medium | Free-tier sizing, autoscaling floors, budget alarms, single region |
| R6 | Security defect in auth | Severe | Low–Medium | Standard libraries only, ASVS checklist, Step 32 review |
| R7 | Migration mistakes in production | Downtime | Low | Expand→migrate→contract, staging rehearsal, tested backups |
| R8 | Single-developer bus factor | Continuity | High | ADRs, README quick-start, documented runbooks (Step 36) |

## Appendix F — Interview / Portfolio Talking Points

1. Why microservices here, and what the boundaries are — plus honest trade-offs versus a modular monolith.
2. Refresh-token rotation with family-based reuse detection, and why access tokens stay in memory.
3. Why webhooks (not client callbacks) are authoritative for payment, and how idempotency is enforced.
4. The outbox pattern: how a payment never enrols twice and never fails to enrol.
5. Scaling WebSocket chat horizontally with a Redis relay, and how ordering is preserved with ULIDs.
6. Cache-aside design, chosen TTLs and the invalidation strategy on publish.
7. Where JPA is right and where raw JDBC wins, with the N+1 problem as the concrete example.
8. The CI/CD gates and what each one prevents from reaching production.
9. The degraded-mode matrix — what still works when Redis, the broker or the transcoder is down.

---

**End of document — ELMS-SRS-001 v1.0**
