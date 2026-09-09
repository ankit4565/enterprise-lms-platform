package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.courseservice.dto.response.EnrolmentResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrolmentServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrolmentService enrolmentService;

    private UUID courseId;
    private UUID studentId;
    private Course freeCourse;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        freeCourse = Course.builder()
                .id(courseId)
                .title("Free Course")
                .slug("free-course")
                .status(CourseStatus.PUBLISHED)
                .priceMinor(0L)
                .enrolmentCount(5)
                .build();

        paidCourse = Course.builder()
                .id(courseId)
                .title("Paid Course")
                .slug("paid-course")
                .status(CourseStatus.PUBLISHED)
                .priceMinor(499900L)
                .enrolmentCount(10)
                .build();
    }

    @Test
    void enrolInCourse_FreeCourse_Success() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(freeCourse));
        when(enrolmentRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(false);
        when(enrolmentRepository.save(any(Enrolment.class))).thenAnswer(invocation -> {
            Enrolment e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EnrolmentResponse response = enrolmentService.enrolInCourse(courseId, studentId, EnrolmentSource.FREE, null);

        assertNotNull(response);
        assertEquals(EnrolmentStatus.ACTIVE, response.getStatus());
        assertEquals(EnrolmentSource.FREE, response.getSource());
        assertEquals(6, freeCourse.getEnrolmentCount());
        verify(courseRepository).save(freeCourse);
    }

    @Test
    void enrolInCourse_Duplicate_ThrowsConflict() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(freeCourse));
        when(enrolmentRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(true);

        assertThrows(ApiException.class, () ->
                enrolmentService.enrolInCourse(courseId, studentId, EnrolmentSource.FREE, null));
    }

    @Test
    void enrolInCourse_PaidCourseWithFreeSource_ThrowsBadRequest() {
        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(paidCourse));
        when(enrolmentRepository.existsByCourseIdAndStudentId(courseId, studentId)).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                enrolmentService.enrolInCourse(courseId, studentId, EnrolmentSource.FREE, null));

        assertTrue(ex.getMessage().contains("paid course"));
    }

    @Test
    void cancelEnrolment_Success_DecrementsCounter() {
        Enrolment enrolment = Enrolment.builder()
                .id(UUID.randomUUID())
                .course(freeCourse)
                .studentId(studentId)
                .source(EnrolmentSource.FREE)
                .status(EnrolmentStatus.ACTIVE)
                .build();

        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));

        enrolmentService.cancelEnrolment(courseId, studentId);

        assertEquals(EnrolmentStatus.CANCELLED, enrolment.getStatus());
        assertEquals(4, freeCourse.getEnrolmentCount());
        verify(enrolmentRepository).save(enrolment);
        verify(courseRepository).save(freeCourse);
    }
}
