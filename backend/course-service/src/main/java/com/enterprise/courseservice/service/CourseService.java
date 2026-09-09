package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.request.CreateCourseRequest;
import com.enterprise.courseservice.dto.request.ReviewCourseRequest;
import com.enterprise.courseservice.dto.request.UpdateCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.dto.response.CourseSummaryResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryService categoryService;
    private final LessonRepository lessonRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public CourseDetailResponse createCourse(UUID instructorId, CreateCourseRequest request) {
        Category category = categoryService.getCategoryEntity(request.getCategoryId());

        String baseSlug = toSlug(request.getTitle());
        String slug = baseSlug;
        int count = 1;
        while (courseRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }

        Course course = Course.builder()
                .instructorId(instructorId)
                .category(category)
                .title(request.getTitle())
                .slug(slug)
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .level(request.getLevel() != null ? request.getLevel() : CourseLevel.ALL)
                .language(request.getLanguage() != null ? request.getLanguage() : "en")
                .thumbnailUrl(request.getThumbnailUrl())
                .promoVideoId(request.getPromoVideoId())
                .priceMinor(request.getPriceMinor() != null ? request.getPriceMinor() : 0L)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .discountPriceMinor(request.getDiscountPriceMinor())
                .status(CourseStatus.DRAFT)
                .tags(formatList(request.getTags(), ","))
                .requirements(formatList(request.getRequirements(), "\n"))
                .outcomes(formatList(request.getOutcomes(), "\n"))
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created with id {} and slug {} by instructor {}", saved.getId(), saved.getSlug(), instructorId);
        return CourseDetailResponse.fromEntity(saved);
    }

    @Transactional
    public CourseDetailResponse updateCourse(UUID courseId, UUID requesterId, List<String> requesterRoles, UpdateCourseRequest request) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new BadRequestException("Archived courses cannot be updated");
        }

        if (request.getTitle() != null && !request.getTitle().equals(course.getTitle())) {
            course.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            course.setSubtitle(request.getSubtitle());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryService.getCategoryEntity(request.getCategoryId());
            course.setCategory(category);
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getLevel() != null) {
            course.setLevel(request.getLevel());
        }
        if (request.getLanguage() != null) {
            course.setLanguage(request.getLanguage());
        }
        if (request.getThumbnailUrl() != null) {
            course.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getPromoVideoId() != null) {
            course.setPromoVideoId(request.getPromoVideoId());
        }
        if (request.getPriceMinor() != null) {
            course.setPriceMinor(request.getPriceMinor());
        }
        if (request.getCurrency() != null) {
            course.setCurrency(request.getCurrency());
        }
        if (request.getDiscountPriceMinor() != null) {
            course.setDiscountPriceMinor(request.getDiscountPriceMinor());
        }
        if (request.getTags() != null) {
            course.setTags(formatList(request.getTags(), ","));
        }
        if (request.getRequirements() != null) {
            course.setRequirements(formatList(request.getRequirements(), "\n"));
        }
        if (request.getOutcomes() != null) {
            course.setOutcomes(formatList(request.getOutcomes(), "\n"));
        }

        Course saved = courseRepository.save(course);
        return CourseDetailResponse.fromEntity(saved);
    }

    @Transactional
    public CourseDetailResponse submitCourseForReview(UUID courseId, UUID requesterId, List<String> requesterRoles) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED courses can be submitted for review. Current status: " + course.getStatus());
        }

        // FR-CRS-06 Publication validation guards
        validateCourseForSubmission(course);

        course.setStatus(CourseStatus.PENDING_REVIEW);
        Course saved = courseRepository.save(course);
        log.info("Course {} submitted for review by instructor {}", courseId, requesterId);
        return CourseDetailResponse.fromEntity(saved);
    }

    private void validateCourseForSubmission(Course course) {
        List<String> errors = new ArrayList<>();

        if (course.getDescription() == null || course.getDescription().trim().length() < 200) {
            errors.add("Course description must be at least 200 characters (current: " +
                    (course.getDescription() != null ? course.getDescription().trim().length() : 0) + ")");
        }

        if (!StringUtils.hasText(course.getThumbnailUrl())) {
            errors.add("Course thumbnail is required for publishing");
        }

        if (course.getModules() == null || course.getModules().isEmpty()) {
            errors.add("Course must have at least 1 module");
        }

        long readyLessonsCount = lessonRepository.countByCourseIdAndIsPublishedTrue(course.getId());
        if (readyLessonsCount < 1) {
            errors.add("Course must contain at least 1 published-ready lesson");
        }

        if (course.getPriceMinor() == null || course.getPriceMinor() < 0) {
            errors.add("Course price cannot be negative");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Course publishing validation failed: " + String.join("; ", errors));
        }
    }

    @Transactional
    public CourseDetailResponse reviewCourse(UUID courseId, ReviewCourseRequest request) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getStatus() != CourseStatus.PENDING_REVIEW) {
            throw new BadRequestException("Course is not in PENDING_REVIEW status. Current status: " + course.getStatus());
        }

        if ("APPROVED".equalsIgnoreCase(request.getDecision())) {
            course.setStatus(CourseStatus.PUBLISHED);
            course.setPublishedAt(Instant.now());
            course.setRejectedReason(null);
            log.info("Course {} approved and PUBLISHED", courseId);
        } else if ("REJECTED".equalsIgnoreCase(request.getDecision())) {
            if (!StringUtils.hasText(request.getReason())) {
                throw new BadRequestException("Rejection reason is mandatory when rejecting a course");
            }
            course.setStatus(CourseStatus.REJECTED);
            course.setRejectedReason(request.getReason().trim());
            log.info("Course {} REJECTED. Reason: {}", courseId, request.getReason());
        }

        Course saved = courseRepository.save(course);
        return CourseDetailResponse.fromEntity(saved);
    }

    @Transactional
    public void archiveCourse(UUID courseId, UUID requesterId, List<String> requesterRoles) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);
        course.setStatus(CourseStatus.ARCHIVED);
        course.setDeletedAt(Instant.now());
        courseRepository.save(course);
        log.info("Course {} archived", courseId);
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(String slugOrId, UUID requesterId, List<String> requesterRoles) {
        Course course = resolveCourse(slugOrId);

        boolean isOwner = requesterId != null && requesterId.equals(course.getInstructorId());
        boolean isAdmin = requesterRoles != null && (requesterRoles.contains("ADMIN") || requesterRoles.contains("SUPER_ADMIN"));

        if (course.getStatus() != CourseStatus.PUBLISHED && !isOwner && !isAdmin) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Course is not published");
        }

        return CourseDetailResponse.fromEntity(course);
    }

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> searchCatalogue(
            String keyword,
            UUID categoryId,
            CourseLevel level,
            String language,
            Long minPrice,
            Long maxPrice,
            Pageable pageable
    ) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), CourseStatus.PUBLISHED));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate subtitleMatch = cb.like(cb.lower(root.get("subtitle")), likePattern);
                Predicate tagsMatch = cb.like(cb.lower(root.get("tags")), likePattern);
                predicates.add(cb.or(titleMatch, subtitleMatch, tagsMatch));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (level != null && level != CourseLevel.ALL) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (StringUtils.hasText(language)) {
                predicates.add(cb.equal(cb.lower(root.get("language")), language.trim().toLowerCase()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("priceMinor"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceMinor"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return courseRepository.findAll(spec, pageable).map(CourseSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> getInstructorCourses(UUID instructorId, Pageable pageable) {
        return courseRepository.findByInstructorIdAndDeletedAtIsNull(instructorId, pageable)
                .map(CourseSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> getPendingCourses(Pageable pageable) {
        return courseRepository.findByStatusAndDeletedAtIsNull(CourseStatus.PENDING_REVIEW, pageable)
                .map(CourseSummaryResponse::fromEntity);
    }

    private Course resolveCourse(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return courseRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        } catch (IllegalArgumentException e) {
            return courseRepository.findBySlugAndDeletedAtIsNull(slugOrId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with slug: " + slugOrId));
        }
    }

    private Course verifyCourseOwnership(UUID courseId, UUID requesterId, List<String> requesterRoles) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isAdmin = requesterRoles != null && (requesterRoles.contains("ADMIN") || requesterRoles.contains("SUPER_ADMIN"));
        if (!isAdmin && (requesterId == null || !requesterId.equals(course.getInstructorId()))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You are not authorized to modify this course");
        }
        return course;
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "course";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private static String formatList(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(delimiter, list);
    }
}
