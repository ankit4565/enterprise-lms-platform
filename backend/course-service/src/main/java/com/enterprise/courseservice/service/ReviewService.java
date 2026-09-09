package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.request.CreateReviewRequest;
import com.enterprise.courseservice.dto.request.UpdateReviewRequest;
import com.enterprise.courseservice.dto.response.ReviewResponse;
import com.enterprise.courseservice.entity.Course;
import com.enterprise.courseservice.entity.EnrolmentStatus;
import com.enterprise.courseservice.entity.Review;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import com.enterprise.courseservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;

    @Transactional
    public ReviewResponse createReview(UUID courseId, UUID studentId, CreateReviewRequest request) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .filter(e -> e.getStatus() == EnrolmentStatus.ACTIVE || e.getStatus() == EnrolmentStatus.COMPLETED)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "Only enrolled students can review this course"));

        if (reviewRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ApiException(ErrorCode.CONFLICT, "You have already reviewed this course");
        }

        Review review = Review.builder()
                .course(course)
                .studentId(studentId)
                .rating(request.getRating())
                .comment(request.getComment())
                .isApproved(true)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Review created for course {} by student {}", courseId, studentId);

        recalculateCourseRating(course);

        return ReviewResponse.fromEntity(saved);
    }

    @Transactional
    public ReviewResponse updateReview(UUID courseId, UUID studentId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for student in course: " + courseId));

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        Review saved = reviewRepository.save(review);

        recalculateCourseRating(review.getCourse());

        return ReviewResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteReview(UUID courseId, UUID studentId, List<String> requesterRoles) {
        Review review = reviewRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for student in course: " + courseId));

        boolean isAdmin = requesterRoles != null && (requesterRoles.contains("ADMIN") || requesterRoles.contains("SUPER_ADMIN"));
        if (!review.getStudentId().equals(studentId) && !isAdmin) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You are not authorized to delete this review");
        }

        Course course = review.getCourse();
        reviewRepository.delete(review);
        log.info("Review deleted for course {}", courseId);

        recalculateCourseRating(course);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCourseReviews(UUID courseId, Pageable pageable) {
        return reviewRepository.findByCourseIdAndIsApprovedTrue(courseId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    private void recalculateCourseRating(Course course) {
        Double avgRating = reviewRepository.findAverageRatingByCourseId(course.getId());
        Long count = reviewRepository.countApprovedReviewsByCourseId(course.getId());

        course.setRatingAvg(BigDecimal.valueOf(avgRating != null ? avgRating : 0.0).setScale(2, RoundingMode.HALF_UP));
        course.setRatingCount(count != null ? count.intValue() : 0);
        courseRepository.save(course);
    }
}
