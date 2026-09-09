package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.request.UpdateProgressRequest;
import com.enterprise.courseservice.dto.response.CourseProgressResponse;
import com.enterprise.courseservice.dto.response.LessonProgressResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import com.enterprise.courseservice.repository.LessonProgressRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final EnrolmentRepository enrolmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Transactional
    public LessonProgressResponse recordProgress(UUID courseId, UUID lessonId, UUID studentId, UpdateProgressRequest request) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "You must be enrolled to track lesson progress"));

        if (enrolment.getStatus() != EnrolmentStatus.ACTIVE && enrolment.getStatus() != EnrolmentStatus.COMPLETED) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Enrolment is not active");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        if (!lesson.getCourseId().equals(courseId)) {
            throw new ResourceNotFoundException("Lesson does not belong to course: " + courseId);
        }

        LessonProgress progress = lessonProgressRepository.findByEnrolmentIdAndLessonId(enrolment.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder()
                        .enrolment(enrolment)
                        .lesson(lesson)
                        .status(ProgressStatus.NOT_STARTED)
                        .lastPositionSeconds(0)
                        .watchedSeconds(0)
                        .build());

        if (request.getLastPositionSeconds() != null) {
            progress.setLastPositionSeconds(request.getLastPositionSeconds());
        }

        if (request.getWatchedSeconds() != null) {
            int currentWatched = progress.getWatchedSeconds() != null ? progress.getWatchedSeconds() : 0;
            progress.setWatchedSeconds(Math.max(currentWatched, request.getWatchedSeconds()));
        }

        boolean shouldAutoComplete = false;
        if (lesson.getType() == LessonType.VIDEO && lesson.getDurationSeconds() > 0) {
            double watchRatio = (double) progress.getWatchedSeconds() / lesson.getDurationSeconds();
            if (watchRatio >= 0.90) {
                shouldAutoComplete = true;
            }
        }

        if (Boolean.TRUE.equals(request.getIsCompleted()) || shouldAutoComplete) {
            if (progress.getStatus() != ProgressStatus.COMPLETED) {
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setCompletedAt(Instant.now());
            }
        } else if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }

        LessonProgress saved = lessonProgressRepository.save(progress);

        recalculateEnrolmentProgress(enrolment, courseId);

        return LessonProgressResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(UUID courseId, UUID studentId) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "You must be enrolled to view course progress"));

        List<LessonProgressResponse> lessonProgresses = lessonProgressRepository.findByEnrolmentId(enrolment.getId())
                .stream()
                .map(LessonProgressResponse::fromEntity)
                .collect(Collectors.toList());

        long totalLessons = lessonRepository.countByCourseIdAndIsPublishedTrue(courseId);
        long completedLessons = lessonProgressRepository.countByEnrolmentIdAndStatus(enrolment.getId(), ProgressStatus.COMPLETED);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(enrolment.getCourse() != null ? enrolment.getCourse().getTitle() : null)
                .enrolmentId(enrolment.getId())
                .enrolmentStatus(enrolment.getStatus())
                .progressPercent(enrolment.getProgressPercent())
                .completedLessonsCount((int) completedLessons)
                .totalLessonsCount((int) totalLessons)
                .completedAt(enrolment.getCompletedAt())
                .lastAccessedAt(enrolment.getLastAccessedAt())
                .lessonProgresses(lessonProgresses)
                .build();
    }

    private void recalculateEnrolmentProgress(Enrolment enrolment, UUID courseId) {
        long totalLessons = lessonRepository.countByCourseIdAndIsPublishedTrue(courseId);
        long completedLessons = lessonProgressRepository.countByEnrolmentIdAndStatus(enrolment.getId(), ProgressStatus.COMPLETED);

        BigDecimal progressPercent = BigDecimal.ZERO;
        if (totalLessons > 0) {
            progressPercent = BigDecimal.valueOf((double) completedLessons / totalLessons * 100)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        enrolment.setProgressPercent(progressPercent);
        enrolment.setLastAccessedAt(Instant.now());

        if (totalLessons > 0 && completedLessons >= totalLessons) {
            if (enrolment.getStatus() != EnrolmentStatus.COMPLETED) {
                enrolment.setStatus(EnrolmentStatus.COMPLETED);
                enrolment.setCompletedAt(Instant.now());
                log.info("Course {} completed by student {}", courseId, enrolment.getStudentId());
            }
        }

        enrolmentRepository.save(enrolment);
    }
}
