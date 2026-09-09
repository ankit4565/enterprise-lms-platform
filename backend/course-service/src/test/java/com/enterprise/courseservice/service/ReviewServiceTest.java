package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.courseservice.dto.request.CreateReviewRequest;
import com.enterprise.courseservice.dto.request.UpdateReviewRequest;
import com.enterprise.courseservice.dto.response.ReviewResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import com.enterprise.courseservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @InjectMocks
    private ReviewService reviewService;

    private UUID courseId;
    private UUID studentId;
    private Course course;
    private Enrolment enrolment;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        course = Course.builder()
                .id(courseId)
                .title("Advanced React")
                .status(CourseStatus.PUBLISHED)
                .ratingAvg(BigDecimal.ZERO)
                .ratingCount(0)
                .build();

        enrolment = Enrolment.builder()
                .id(UUID.randomUUID())
                .course(course)
                .studentId(studentId)
                .status(EnrolmentStatus.ACTIVE)
                .build();
    }

    @Test
    void createReview_UnenrolledStudent_ThrowsAccessDenied() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.empty());

        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).comment("Great!").build();

        assertThrows(ApiException.class, () -> reviewService.createReview(courseId, studentId, request));
    }

    @Test
    void createReview_Duplicate_ThrowsConflict() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(reviewRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(true);

        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).comment("Great!").build();

        assertThrows(ApiException.class, () -> reviewService.createReview(courseId, studentId, request));
    }

    @Test
    void createReview_Success_RecalculatesRating() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(reviewRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(reviewRepository.findAverageRatingByCourseId(courseId)).thenReturn(5.0);
        when(reviewRepository.countApprovedReviewsByCourseId(courseId)).thenReturn(1L);

        CreateReviewRequest request = CreateReviewRequest.builder().rating(5).comment("Exceptional course!").build();

        ReviewResponse response = reviewService.createReview(courseId, studentId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals(BigDecimal.valueOf(5.0).setScale(2), course.getRatingAvg());
        assertEquals(1, course.getRatingCount());
        verify(courseRepository).save(course);
    }

    @Test
    void updateReview_Success_RecalculatesRating() {
        Review existing = Review.builder()
                .id(UUID.randomUUID())
                .course(course)
                .studentId(studentId)
                .rating(4)
                .comment("Good")
                .build();

        when(reviewRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findAverageRatingByCourseId(courseId)).thenReturn(5.0);
        when(reviewRepository.countApprovedReviewsByCourseId(courseId)).thenReturn(1L);

        UpdateReviewRequest request = UpdateReviewRequest.builder().rating(5).comment("Updated: Amazing!").build();

        ReviewResponse response = reviewService.updateReview(courseId, studentId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        verify(courseRepository).save(course);
    }
}
