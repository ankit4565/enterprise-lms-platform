package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.courseservice.dto.request.CreateCourseRequest;
import com.enterprise.courseservice.dto.request.ReviewCourseRequest;
import com.enterprise.courseservice.dto.request.UpdateCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.entity.Module;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private CourseService courseService;

    private UUID courseId;
    private UUID instructorId;
    private UUID categoryId;
    private Category category;
    private Course course;

    private static final String LONG_DESCRIPTION =
            "This is a comprehensive in-depth course designed for software engineers and developers. " +
            "You will learn enterprise microservices patterns, security architectures, domain-driven design, " +
            "testing best practices, and scalable cloud deployment strategies using Spring Boot and Docker.";

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        instructorId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("Computer Science")
                .slug("computer-science")
                .build();

        course = Course.builder()
                .id(courseId)
                .instructorId(instructorId)
                .category(category)
                .title("Mastering Microservices")
                .slug("mastering-microservices")
                .description(LONG_DESCRIPTION)
                .thumbnailUrl("https://example.com/thumb.png")
                .priceMinor(499900L)
                .currency("INR")
                .status(CourseStatus.DRAFT)
                .modules(new ArrayList<>())
                .build();
    }

    @Test
    void createCourse_Success_InitialStatusDraft() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .title("Mastering Microservices")
                .categoryId(categoryId)
                .description(LONG_DESCRIPTION)
                .thumbnailUrl("https://example.com/thumb.png")
                .priceMinor(499900L)
                .build();

        when(categoryService.getCategoryEntity(categoryId)).thenReturn(category);
        when(courseRepository.existsBySlug("mastering-microservices")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(courseId);
            return c;
        });

        CourseDetailResponse response = courseService.createCourse(instructorId, request);

        assertNotNull(response);
        assertEquals("Mastering Microservices", response.getTitle());
        assertEquals("mastering-microservices", response.getSlug());
        assertEquals(CourseStatus.DRAFT, response.getStatus());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void submitCourseForReview_FailsWhenDescriptionTooShort() {
        course.setDescription("Too short description");
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                courseService.submitCourseForReview(courseId, instructorId, List.of("INSTRUCTOR")));

        assertTrue(ex.getMessage().contains("at least 200 characters"));
    }

    @Test
    void submitCourseForReview_FailsWhenNoModules() {
        course.setModules(Collections.emptyList());
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                courseService.submitCourseForReview(courseId, instructorId, List.of("INSTRUCTOR")));

        assertTrue(ex.getMessage().contains("at least 1 module"));
    }

    @Test
    void submitCourseForReview_FailsWhenNoPublishedLessons() {
        Module module = Module.builder().id(UUID.randomUUID()).title("Mod 1").position(1).build();
        course.setModules(List.of(module));

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseIdAndIsPublishedTrue(courseId)).thenReturn(0L);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                courseService.submitCourseForReview(courseId, instructorId, List.of("INSTRUCTOR")));

        assertTrue(ex.getMessage().contains("at least 1 published-ready lesson"));
    }

    @Test
    void submitCourseForReview_Success() {
        Module module = Module.builder().id(UUID.randomUUID()).title("Mod 1").position(1).build();
        course.setModules(List.of(module));

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.countByCourseIdAndIsPublishedTrue(courseId)).thenReturn(2L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseDetailResponse response = courseService.submitCourseForReview(courseId, instructorId, List.of("INSTRUCTOR"));

        assertNotNull(response);
        assertEquals(CourseStatus.PENDING_REVIEW, response.getStatus());
    }

    @Test
    void reviewCourse_Approve_Success() {
        course.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCourseRequest request = ReviewCourseRequest.builder().decision("APPROVED").build();

        CourseDetailResponse response = courseService.reviewCourse(courseId, request);

        assertEquals(CourseStatus.PUBLISHED, response.getStatus());
        assertNotNull(course.getPublishedAt());
    }

    @Test
    void reviewCourse_RejectWithoutReason_ThrowsBadRequest() {
        course.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

        ReviewCourseRequest request = ReviewCourseRequest.builder().decision("REJECTED").reason(" ").build();

        assertThrows(BadRequestException.class, () -> courseService.reviewCourse(courseId, request));
    }

    @Test
    void reviewCourse_RejectWithReason_Success() {
        course.setStatus(CourseStatus.PENDING_REVIEW);
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCourseRequest request = ReviewCourseRequest.builder()
                .decision("REJECTED")
                .reason("Audio quality is poor in module 1")
                .build();

        CourseDetailResponse response = courseService.reviewCourse(courseId, request);

        assertEquals(CourseStatus.REJECTED, response.getStatus());
        assertEquals("Audio quality is poor in module 1", course.getRejectedReason());
    }

    @Test
    void getCourseDetail_DraftNotAccessibleByStranger() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        UUID strangerId = UUID.randomUUID();

        assertThrows(ApiException.class, () ->
                courseService.getCourseDetail(courseId.toString(), strangerId, List.of("STUDENT")));
    }
}
