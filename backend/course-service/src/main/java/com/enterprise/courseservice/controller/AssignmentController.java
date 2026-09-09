package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.courseservice.dto.request.CreateAssignmentRequest;
import com.enterprise.courseservice.dto.request.GradeSubmissionRequest;
import com.enterprise.courseservice.dto.request.SubmitAssignmentRequest;
import com.enterprise.courseservice.dto.request.UpdateAssignmentRequest;
import com.enterprise.courseservice.dto.response.AssignmentResponse;
import com.enterprise.courseservice.dto.response.SubmissionResponse;
import com.enterprise.courseservice.entity.SubmissionStatus;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Assignments", description = "Assignment authoring, student submission, and grading queue")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/lessons/{lessonId}/assignments")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Create an assignment for a lesson (Instructor)")
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        AssignmentResponse response = assignmentService.createAssignment(lessonId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Assignment created successfully"));
    }

    @PutMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Update an assignment (Instructor)")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAssignmentRequest request
    ) {
        AssignmentResponse response = assignmentService.updateAssignment(id, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Assignment updated successfully"));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Get assignment by ID (Enrolled Student or Instructor)")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isInstructor = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        AssignmentResponse response = assignmentService.getAssignment(id, principal.getUserId(), isInstructor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Assignment retrieved successfully"));
    }

    @GetMapping("/lessons/{lessonId}/assignment")
    @Operation(summary = "Get assignment by Lesson ID")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentByLesson(
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isInstructor = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        AssignmentResponse response = assignmentService.getAssignmentByLessonId(lessonId, principal.getUserId(), isInstructor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Assignment retrieved successfully"));
    }

    @PostMapping("/assignments/{id}/submissions")
    @Operation(summary = "Submit an assignment solution (Student)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitAssignmentRequest request
    ) {
        SubmissionResponse response = assignmentService.submitAssignment(id, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Assignment submitted successfully"));
    }

    @GetMapping("/assignments/{id}/submissions/my")
    @Operation(summary = "Get current student's latest submission for an assignment")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getMySubmission(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SubmissionResponse response = assignmentService.getMySubmission(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Submission retrieved successfully"));
    }

    @GetMapping("/assignments/{id}/submissions/history")
    @Operation(summary = "Get student's full submission attempt history")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getMySubmissionHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<SubmissionResponse> history = assignmentService.getMySubmissionHistory(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(history, "Submission history retrieved successfully"));
    }

    @GetMapping("/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Get submissions grading queue for an assignment (Instructor)")
    public ResponseEntity<ApiResponse<PageResponse<SubmissionResponse>>> getGradingQueue(
            @PathVariable UUID id,
            @RequestParam(required = false) SubmissionStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SubmissionResponse> page = assignmentService.getGradingQueue(id, principal.getUserId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page), "Submissions grading queue retrieved"));
    }

    @PostMapping("/assignments/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @Operation(summary = "Grade a student submission with score and feedback (Instructor)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @PathVariable UUID submissionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GradeSubmissionRequest request
    ) {
        SubmissionResponse response = assignmentService.gradeSubmission(submissionId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Submission graded successfully"));
    }
}
