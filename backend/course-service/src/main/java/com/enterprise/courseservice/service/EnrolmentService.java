package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.response.EnrolmentResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrolmentService {

    private final EnrolmentRepository enrolmentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public EnrolmentResponse enrolInCourse(UUID courseId, UUID studentId, EnrolmentSource source, UUID orderId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException("Cannot enrol in an unapproved or unpublished course");
        }

        if (enrolmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ApiException(ErrorCode.CONFLICT, "Already enrolled in this course");
        }

        EnrolmentSource effectiveSource = source != null ? source : EnrolmentSource.FREE;

        if (course.getPriceMinor() > 0 && effectiveSource == EnrolmentSource.FREE) {
            throw new BadRequestException("This is a paid course and requires payment");
        }

        Enrolment enrolment = Enrolment.builder()
                .course(course)
                .studentId(studentId)
                .source(effectiveSource)
                .orderId(orderId)
                .status(EnrolmentStatus.ACTIVE)
                .progressPercent(BigDecimal.ZERO)
                .enrolledAt(Instant.now())
                .lastAccessedAt(Instant.now())
                .build();

        Enrolment saved = enrolmentRepository.save(enrolment);

        course.setEnrolmentCount(course.getEnrolmentCount() + 1);
        courseRepository.save(course);

        log.info("Student {} enrolled in course {} via {}", studentId, courseId, effectiveSource);
        return EnrolmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<EnrolmentResponse> getMyEnrolments(UUID studentId, Pageable pageable) {
        return enrolmentRepository.findByStudentId(studentId, pageable)
                .map(EnrolmentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public EnrolmentResponse getEnrolment(UUID courseId, UUID studentId) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrolment not found for student in course: " + courseId));
        return EnrolmentResponse.fromEntity(enrolment);
    }

    @Transactional
    public void cancelEnrolment(UUID courseId, UUID studentId) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrolment not found for student in course: " + courseId));

        if (enrolment.getSource() == EnrolmentSource.PURCHASE) {
            throw new BadRequestException("Paid course unenrolments must follow the refund policy");
        }

        if (enrolment.getStatus() == EnrolmentStatus.CANCELLED) {
            return;
        }

        enrolment.setStatus(EnrolmentStatus.CANCELLED);
        enrolmentRepository.save(enrolment);

        Course course = enrolment.getCourse();
        if (course != null && course.getEnrolmentCount() > 0) {
            course.setEnrolmentCount(course.getEnrolmentCount() - 1);
            courseRepository.save(course);
        }

        log.info("Enrolment cancelled for student {} in course {}", studentId, courseId);
    }
}
