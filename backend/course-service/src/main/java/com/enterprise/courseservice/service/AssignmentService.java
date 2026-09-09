package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.courseservice.dto.request.CreateAssignmentRequest;
import com.enterprise.courseservice.dto.request.GradeSubmissionRequest;
import com.enterprise.courseservice.dto.request.SubmitAssignmentRequest;
import com.enterprise.courseservice.dto.request.UpdateAssignmentRequest;
import com.enterprise.courseservice.dto.response.AssignmentResponse;
import com.enterprise.courseservice.dto.response.SubmissionResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.repository.AssignmentRepository;
import com.enterprise.courseservice.repository.AssignmentSubmissionRepository;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.EnrolmentRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;

    @Transactional
    public AssignmentResponse createAssignment(UUID lessonId, UUID instructorId, CreateAssignmentRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found: " + lessonId));

        Course course = courseRepository.findById(lesson.getCourseId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found: " + lesson.getCourseId()));

        if (!course.getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can create assignments");
        }

        if (assignmentRepository.existsByLessonId(lessonId)) {
            throw new ApiException(ErrorCode.CONFLICT, "An assignment already exists for this lesson");
        }

        // Set lesson type to ASSIGNMENT if not already
        if (lesson.getType() != LessonType.ASSIGNMENT) {
            lesson.setType(LessonType.ASSIGNMENT);
            lessonRepository.save(lesson);
        }

        Assignment assignment = Assignment.builder()
                .lesson(lesson)
                .course(course)
                .title(request.getTitle())
                .instructions(request.getInstructions())
                .maxScore(request.getMaxScore() != null ? request.getMaxScore() : 100)
                .dueAt(request.getDueAt())
                .allowLate(request.getAllowLate() != null ? request.getAllowLate() : true)
                .latePenaltyPercent(request.getLatePenaltyPercent() != null ? request.getLatePenaltyPercent() : 0)
                .allowedFileTypes(request.getAllowedFileTypes())
                .maxFileSizeMb(request.getMaxFileSizeMb() != null ? request.getMaxFileSizeMb() : 50)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment created with id {} for lesson {} in course {}", saved.getId(), lessonId, course.getId());
        return AssignmentResponse.from(saved);
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID assignmentId, UUID instructorId, UpdateAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

        if (!assignment.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can update this assignment");
        }

        if (request.getTitle() != null) assignment.setTitle(request.getTitle());
        if (request.getInstructions() != null) assignment.setInstructions(request.getInstructions());
        if (request.getMaxScore() != null) assignment.setMaxScore(request.getMaxScore());
        if (request.getDueAt() != null) assignment.setDueAt(request.getDueAt());
        if (request.getAllowLate() != null) assignment.setAllowLate(request.getAllowLate());
        if (request.getLatePenaltyPercent() != null) assignment.setLatePenaltyPercent(request.getLatePenaltyPercent());
        if (request.getAllowedFileTypes() != null) assignment.setAllowedFileTypes(request.getAllowedFileTypes());
        if (request.getMaxFileSizeMb() != null) assignment.setMaxFileSizeMb(request.getMaxFileSizeMb());

        Assignment updated = assignmentRepository.save(assignment);
        log.info("Assignment {} updated by instructor {}", assignmentId, instructorId);
        return AssignmentResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID assignmentId, UUID userId, boolean isInstructor) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

        if (!isInstructor && !assignment.getCourse().getInstructorId().equals(userId)) {
            verifyEnrolment(assignment.getCourse().getId(), userId);
        }

        return AssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentByLessonId(UUID lessonId, UUID userId, boolean isInstructor) {
        Assignment assignment = assignmentRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No assignment found for lesson: " + lessonId));

        if (!isInstructor && !assignment.getCourse().getInstructorId().equals(userId)) {
            verifyEnrolment(assignment.getCourse().getId(), userId);
        }

        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public SubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, SubmitAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

        verifyEnrolment(assignment.getCourse().getId(), studentId);

        boolean isLate = false;
        if (assignment.getDueAt() != null && Instant.now().isAfter(assignment.getDueAt())) {
            if (Boolean.FALSE.equals(assignment.getAllowLate())) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Late submissions are not accepted for this assignment");
            }
            isLate = true;
        }

        // Check previous attempts
        Optional<AssignmentSubmission> latestOpt = submissionRepository
                .findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(assignmentId, studentId);

        int nextAttempt = latestOpt.map(sub -> sub.getAttemptNo() + 1).orElse(1);

        AssignmentSubmission submission = AssignmentSubmission.builder()
                .assignment(assignment)
                .studentId(studentId)
                .attemptNo(nextAttempt)
                .textAnswer(request.getTextAnswer())
                .fileUrls(request.getFileUrls())
                .status(SubmissionStatus.SUBMITTED)
                .isLate(isLate)
                .submittedAt(Instant.now())
                .build();

        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Student {} submitted attempt {} for assignment {} (isLate={})",
                studentId, nextAttempt, assignmentId, isLate);
        return SubmissionResponse.from(saved);
    }

    @Transactional
    public SubmissionResponse gradeSubmission(UUID submissionId, UUID instructorId, GradeSubmissionRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Submission not found: " + submissionId));

        Assignment assignment = submission.getAssignment();
        if (!assignment.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can grade submissions");
        }

        if (request.getScore().compareTo(BigDecimal.valueOf(assignment.getMaxScore())) > 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Score cannot exceed the maximum score of " + assignment.getMaxScore());
        }

        BigDecimal rawScore = request.getScore();
        BigDecimal finalScore = rawScore;

        // Apply late penalty deduction if applicable
        if (Boolean.TRUE.equals(submission.getIsLate()) && assignment.getLatePenaltyPercent() != null && assignment.getLatePenaltyPercent() > 0) {
            BigDecimal multiplier = BigDecimal.valueOf(100 - assignment.getLatePenaltyPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            finalScore = rawScore.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }

        submission.setRawScore(rawScore);
        submission.setFinalScore(finalScore);
        submission.setFeedback(request.getFeedback());
        submission.setStatus(request.getStatus() != null ? request.getStatus() : SubmissionStatus.GRADED);
        submission.setGradedBy(instructorId);
        submission.setGradedAt(Instant.now());

        AssignmentSubmission updated = submissionRepository.save(submission);
        log.info("Submission {} graded by instructor {}. Raw score: {}, Final score: {}, Status: {}",
                submissionId, instructorId, rawScore, finalScore, submission.getStatus());
        return SubmissionResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(UUID assignmentId, UUID studentId) {
        AssignmentSubmission submission = submissionRepository
                .findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(assignmentId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No submission found for this assignment"));
        return SubmissionResponse.from(submission);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMySubmissionHistory(UUID assignmentId, UUID studentId) {
        return submissionRepository.findAllByAssignmentIdAndStudentIdOrderByAttemptNoAsc(assignmentId, studentId)
                .stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> getGradingQueue(UUID assignmentId, UUID instructorId, SubmissionStatus status, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

        if (!assignment.getCourse().getInstructorId().equals(instructorId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the course instructor can view the grading queue");
        }

        Page<AssignmentSubmission> page = (status != null)
                ? submissionRepository.findAllByAssignmentIdAndStatus(assignmentId, status, pageable)
                : submissionRepository.findAllByAssignmentId(assignmentId, pageable);

        return page.map(SubmissionResponse::from);
    }

    private void verifyEnrolment(UUID courseId, UUID studentId) {
        Enrolment enrolment = enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "You must be enrolled in this course to access assignments"));

        if (enrolment.getStatus() != EnrolmentStatus.ACTIVE && enrolment.getStatus() != EnrolmentStatus.COMPLETED) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Enrolment is no longer active (status: " + enrolment.getStatus() + ")");
        }
    }
}
