package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.*;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Quiz authoring, question management, student attempt engine, and auto-grading")
public class QuizController {

    private final QuizService quizService;

    // =========================================================================
    // Instructor Authoring Endpoints
    // =========================================================================

    @PostMapping("/lessons/{lessonId}/quizzes")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create a quiz for a lesson (Instructor)")
    public ResponseEntity<ApiResponse<QuizSummaryResponse>> createQuiz(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateQuizRequest request
    ) {
        QuizSummaryResponse response = quizService.createQuiz(lessonId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Quiz created successfully"));
    }

    @PutMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update quiz settings and metadata (Instructor)")
    public ResponseEntity<ApiResponse<QuizSummaryResponse>> updateQuiz(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateQuizRequest request
    ) {
        QuizSummaryResponse response = quizService.updateQuiz(id, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz updated successfully"));
    }

    @PostMapping("/quizzes/{id}/questions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Add a question with options to a quiz (Instructor)")
    public ResponseEntity<ApiResponse<QuizQuestionAdminResponse>> addQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        QuizQuestionAdminResponse response = quizService.addQuestion(id, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Question added successfully"));
    }

    @DeleteMapping("/quizzes/questions/{questionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Delete a question from a quiz (Instructor)")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        quizService.deleteQuestion(questionId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Question deleted successfully"));
    }

    @GetMapping("/quizzes/{id}/instructor-view")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get full quiz detail with questions and correct answers (Instructor)")
    public ResponseEntity<ApiResponse<QuizDetailResponse>> getQuizInstructorView(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        QuizDetailResponse response = quizService.getQuizInstructorView(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz detail retrieved successfully"));
    }

    // =========================================================================
    // Quiz Metadata & Lesson Association Endpoints
    // =========================================================================

    @GetMapping("/quizzes/{id}")
    @Operation(summary = "Get quiz summary information (Enrolled student or Instructor)")
    public ResponseEntity<ApiResponse<QuizSummaryResponse>> getQuizSummary(
            @PathVariable UUID id
    ) {
        QuizSummaryResponse response = quizService.getQuizSummary(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz summary retrieved successfully"));
    }

    @GetMapping("/lessons/{lessonId}/quiz")
    @Operation(summary = "Get quiz associated with a lesson")
    public ResponseEntity<ApiResponse<QuizSummaryResponse>> getQuizByLesson(
            @PathVariable UUID lessonId
    ) {
        QuizSummaryResponse response = quizService.getQuizByLessonId(lessonId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz retrieved successfully"));
    }

    // =========================================================================
    // Student Quiz Taking Flow
    // =========================================================================

    @PostMapping("/quizzes/{id}/attempts")
    @Operation(summary = "Start or resume a quiz attempt (Student) - Returns sanitized questions (no answer keys)")
    public ResponseEntity<ApiResponse<ActiveQuizAttemptResponse>> startAttempt(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ActiveQuizAttemptResponse response = quizService.startAttempt(id, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Quiz attempt started successfully"));
    }

    @GetMapping("/quizzes/attempts/{attemptId}")
    @Operation(summary = "Get active quiz attempt questions and autosaved answers (Student)")
    public ResponseEntity<ApiResponse<ActiveQuizAttemptResponse>> getActiveAttempt(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ActiveQuizAttemptResponse response = quizService.getActiveAttempt(attemptId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Active attempt retrieved"));
    }

    @PostMapping("/quizzes/attempts/{attemptId}/answers")
    @Operation(summary = "Autosave answer for a question in an active attempt (Student)")
    public ResponseEntity<ApiResponse<Void>> autosaveAnswer(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AutosaveAnswerRequest request
    ) {
        quizService.autosaveAnswer(attemptId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Answer autosaved successfully"));
    }

    @PostMapping("/quizzes/attempts/{attemptId}/submit")
    @Operation(summary = "Submit quiz attempt for immediate auto-evaluation and scoring (Student)")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitAttempt(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) SubmitQuizRequest request
    ) {
        QuizResultResponse response = quizService.submitAttempt(attemptId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz submitted and evaluated successfully"));
    }

    @GetMapping("/quizzes/attempts/{attemptId}/result")
    @Operation(summary = "Get quiz attempt result, score, and evaluated answers (Student or Instructor)")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getAttemptResult(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isInstructor = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        QuizResultResponse response = quizService.getAttemptResult(attemptId, principal.getUserId(), isInstructor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz result retrieved successfully"));
    }

    @GetMapping("/quizzes/{id}/my-attempts")
    @Operation(summary = "Get all past attempts for a quiz (Current student)")
    public ResponseEntity<ApiResponse<List<QuizAttemptSummaryResponse>>> getMyAttempts(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<QuizAttemptSummaryResponse> response = quizService.getMyAttempts(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Quiz attempts history retrieved"));
    }
}
