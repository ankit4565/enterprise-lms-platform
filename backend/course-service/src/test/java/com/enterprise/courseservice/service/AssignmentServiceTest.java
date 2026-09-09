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
import com.enterprise.courseservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private UUID instructorId;
    private UUID studentId;
    private UUID courseId;
    private UUID lessonId;
    private Course course;
    private Lesson lesson;
    private Assignment assignment;
    private Enrolment enrolment;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        lessonId = UUID.randomUUID();

        course = Course.builder()
                .id(courseId)
                .instructorId(instructorId)
                .title("Full Stack Development")
                .status(CourseStatus.PUBLISHED)
                .build();

        lesson = Lesson.builder()
                .id(lessonId)
                .courseId(courseId)
                .title("Project 1: API Design")
                .type(LessonType.ARTICLE)
                .build();

        assignment = Assignment.builder()
                .id(UUID.randomUUID())
                .course(course)
                .lesson(lesson)
                .title("Submit REST API Code")
                .instructions("Submit link to repo and ZIP file")
                .maxScore(100)
                .dueAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .allowLate(true)
                .latePenaltyPercent(10)
                .allowedFileTypes("zip,pdf")
                .maxFileSizeMb(50)
                .build();

        enrolment = Enrolment.builder()
                .id(UUID.randomUUID())
                .course(course)
                .studentId(studentId)
                .status(EnrolmentStatus.ACTIVE)
                .build();
    }

    @Test
    void createAssignment_Success() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .title("Project 1: API Design")
                .instructions("Build REST endpoints")
                .maxScore(100)
                .dueAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .allowLate(true)
                .latePenaltyPercent(10)
                .allowedFileTypes("zip")
                .maxFileSizeMb(50)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(assignmentRepository.existsByLessonId(lessonId)).thenReturn(false);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(i -> {
            Assignment a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AssignmentResponse response = assignmentService.createAssignment(lessonId, instructorId, request);

        assertNotNull(response);
        assertEquals("Project 1: API Design", response.getTitle());
        assertEquals(LessonType.ASSIGNMENT, lesson.getType());
        verify(lessonRepository).save(lesson);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void createAssignment_Unauthorized_ThrowsAccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .title("Test")
                .instructions("Test")
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.createAssignment(lessonId, otherUserId, request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void createAssignment_Conflict_ThrowsConflict() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .title("Test")
                .instructions("Test")
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(assignmentRepository.existsByLessonId(lessonId)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.createAssignment(lessonId, instructorId, request));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void updateAssignment_Success() {
        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .title("Updated Title")
                .maxScore(120)
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        AssignmentResponse response = assignmentService.updateAssignment(assignment.getId(), instructorId, request);

        assertNotNull(response);
        assertEquals("Updated Title", assignment.getTitle());
        assertEquals(120, assignment.getMaxScore());
    }

    @Test
    void submitAssignment_OnTime_Success() {
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .textAnswer("Here is my answer")
                .fileUrls("http://s3/file.zip")
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(assignment.getId(), studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> {
            AssignmentSubmission s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SubmissionResponse response = assignmentService.submitAssignment(assignment.getId(), studentId, request);

        assertNotNull(response);
        assertEquals(1, response.getAttemptNo());
        assertFalse(response.getIsLate());
        assertEquals(SubmissionStatus.SUBMITTED, response.getStatus());
        verify(submissionRepository).save(any(AssignmentSubmission.class));
    }

    @Test
    void submitAssignment_Resubmission_IncrementsAttemptNo() {
        AssignmentSubmission existing = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(studentId)
                .attemptNo(1)
                .build();

        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .textAnswer("Second attempt")
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(assignment.getId(), studentId))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionResponse response = assignmentService.submitAssignment(assignment.getId(), studentId, request);

        assertNotNull(response);
        assertEquals(2, response.getAttemptNo());
    }

    @Test
    void submitAssignment_NotEnrolled_ThrowsAccessDenied() {
        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder().build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.submitAssignment(assignment.getId(), studentId, request));
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void submitAssignment_LateForbidden_ThrowsInvalidRequest() {
        assignment.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setAllowLate(false);

        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder().build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.submitAssignment(assignment.getId(), studentId, request));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void submitAssignment_LateAllowed_FlagsIsLateTrue() {
        assignment.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setAllowLate(true);

        SubmitAssignmentRequest request = SubmitAssignmentRequest.builder()
                .textAnswer("Late submission")
                .build();

        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrolmentRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.of(enrolment));
        when(submissionRepository.findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(assignment.getId(), studentId))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionResponse response = assignmentService.submitAssignment(assignment.getId(), studentId, request);

        assertNotNull(response);
        assertTrue(response.getIsLate());
    }

    @Test
    void gradeSubmission_WithLatePenalty_AppliesDeduction() {
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(studentId)
                .attemptNo(1)
                .isLate(true)
                .status(SubmissionStatus.SUBMITTED)
                .build();

        // 10% penalty on 90 score -> final score = 90 * 0.9 = 81.00
        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(new BigDecimal("90.00"))
                .feedback("Good effort, but submitted late")
                .status(SubmissionStatus.GRADED)
                .build();

        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

        SubmissionResponse response = assignmentService.gradeSubmission(submission.getId(), instructorId, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("90.00"), response.getRawScore());
        assertEquals(new BigDecimal("81.00"), response.getFinalScore());
        assertEquals(SubmissionStatus.GRADED, response.getStatus());
        assertEquals("Good effort, but submitted late", response.getFeedback());
        assertEquals(instructorId, response.getGradedBy());
    }

    @Test
    void gradeSubmission_ExceedsMaxScore_ThrowsInvalidRequest() {
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(studentId)
                .build();

        GradeSubmissionRequest request = GradeSubmissionRequest.builder()
                .score(new BigDecimal("105.00")) // max is 100
                .build();

        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.gradeSubmission(submission.getId(), instructorId, request));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void getGradingQueue_Success() {
        AssignmentSubmission sub1 = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(UUID.randomUUID())
                .status(SubmissionStatus.SUBMITTED)
                .build();

        Page<AssignmentSubmission> page = new PageImpl<>(List.of(sub1));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(submissionRepository.findAllByAssignmentIdAndStatus(assignment.getId(), SubmissionStatus.SUBMITTED, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<SubmissionResponse> result = assignmentService.getGradingQueue(assignment.getId(), instructorId, SubmissionStatus.SUBMITTED, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
