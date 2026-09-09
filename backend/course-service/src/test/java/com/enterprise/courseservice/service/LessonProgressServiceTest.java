package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.courseservice.dto.request.UpdateProgressRequest;
import com.enterprise.courseservice.dto.response.CourseProgressResponse;
import com.enterprise.courseservice.dto.response.LessonProgressResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import com.enterprise.courseservice.repository.LessonProgressRepository;
import com.enterprise.courseservice.repository.LessonRepository;
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
class LessonProgressServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private LessonProgressService lessonProgressService;

    private UUID courseId;
    private UUID studentId;
    private UUID lessonId;
    private Course course;
    private Enrolment enrolment;
    private Lesson videoLesson;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        lessonId = UUID.randomUUID();

        course = Course.builder()
                .id(courseId)
                .title("Java Architecture")
                .status(CourseStatus.PUBLISHED)
                .build();

        enrolment = Enrolment.builder()
                .id(UUID.randomUUID())
                .course(course)
                .studentId(studentId)
                .status(EnrolmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .build();

        videoLesson = Lesson.builder()
                .id(lessonId)
                .courseId(courseId)
                .title("Intro Video")
                .type(LessonType.VIDEO)
                .durationSeconds(100) // 100 seconds
                .isPublished(true)
                .build();
    }

    @Test
    void recordProgress_NotEnrolled_ThrowsAccessDenied() {
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.empty());

        UpdateProgressRequest request = UpdateProgressRequest.builder().watchedSeconds(10).build();

        assertThrows(ApiException.class, () ->
                lessonProgressService.recordProgress(courseId, lessonId, studentId, request));
    }

    @Test
    void recordProgress_VideoAutoCompletesAt90Percent() {
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(videoLesson));
        when(lessonProgressRepository.findByEnrolmentIdAndLessonId(enrolment.getId(), lessonId)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.countByCourseIdAndIsPublishedTrue(courseId)).thenReturn(1L);
        when(lessonProgressRepository.countByEnrolmentIdAndStatus(enrolment.getId(), ProgressStatus.COMPLETED)).thenReturn(1L);

        UpdateProgressRequest request = UpdateProgressRequest.builder()
                .watchedSeconds(90) // 90/100 = 90%
                .lastPositionSeconds(90)
                .build();

        LessonProgressResponse response = lessonProgressService.recordProgress(courseId, lessonId, studentId, request);

        assertNotNull(response);
        assertEquals(ProgressStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());

        // Also 1 of 1 completed -> enrolment completed
        assertEquals(EnrolmentStatus.COMPLETED, enrolment.getStatus());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), enrolment.getProgressPercent());
        verify(enrolmentRepository).save(enrolment);
    }

    @Test
    void recordProgress_Below90Percent_SetsInProgress() {
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(videoLesson));
        when(lessonProgressRepository.findByEnrolmentIdAndLessonId(enrolment.getId(), lessonId)).thenReturn(Optional.empty());
        when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.countByCourseIdAndIsPublishedTrue(courseId)).thenReturn(2L);
        when(lessonProgressRepository.countByEnrolmentIdAndStatus(enrolment.getId(), ProgressStatus.COMPLETED)).thenReturn(0L);

        UpdateProgressRequest request = UpdateProgressRequest.builder()
                .watchedSeconds(50) // 50/100 = 50%
                .lastPositionSeconds(50)
                .build();

        LessonProgressResponse response = lessonProgressService.recordProgress(courseId, lessonId, studentId, request);

        assertNotNull(response);
        assertEquals(ProgressStatus.IN_PROGRESS, response.getStatus());
        assertNull(response.getCompletedAt());
        assertEquals(BigDecimal.valueOf(0.00).setScale(2), enrolment.getProgressPercent());
    }

    @Test
    void getCourseProgress_ReturnsProgressSummary() {
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(lessonRepository.countByCourseIdAndIsPublishedTrue(courseId)).thenReturn(5L);
        when(lessonProgressRepository.countByEnrolmentIdAndStatus(enrolment.getId(), ProgressStatus.COMPLETED)).thenReturn(2L);

        CourseProgressResponse response = lessonProgressService.getCourseProgress(courseId, studentId);

        assertNotNull(response);
        assertEquals(courseId, response.getCourseId());
        assertEquals(2, response.getCompletedLessonsCount());
        assertEquals(5, response.getTotalLessonsCount());
    }
}
