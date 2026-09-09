package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.courseservice.dto.response.EnrolmentResponse;
import com.enterprise.courseservice.entity.EnrolmentSource;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.EnrolmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Enrolments", description = "Course enrolment lifecycle and student enrolled courses")
public class EnrolmentController {

    private final EnrolmentService enrolmentService;

    @PostMapping("/{courseId}/enrol")
    @Operation(summary = "Enrol current student in course")
    public ResponseEntity<ApiResponse<EnrolmentResponse>> enrol(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "FREE") EnrolmentSource source,
            @RequestParam(required = false) UUID orderId
    ) {
        EnrolmentResponse enrolment = enrolmentService.enrolInCourse(courseId, principal.getUserId(), source, orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(enrolment, "Enrolled in course successfully"));
    }

    @GetMapping("/enrolments/me")
    @Operation(summary = "Get enrolled courses for current student")
    public ResponseEntity<ApiResponse<PageResponse<EnrolmentResponse>>> getMyEnrolments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 12, sort = "lastAccessedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<EnrolmentResponse> enrolments = enrolmentService.getMyEnrolments(principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(enrolments), "Enrolments retrieved successfully"));
    }

    @GetMapping("/{courseId}/enrolment")
    @Operation(summary = "Get student enrolment details for course")
    public ResponseEntity<ApiResponse<EnrolmentResponse>> getEnrolment(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EnrolmentResponse enrolment = enrolmentService.getEnrolment(courseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(enrolment, "Enrolment details retrieved successfully"));
    }

    @DeleteMapping("/{courseId}/enrol")
    @Operation(summary = "Cancel enrolment in free course")
    public ResponseEntity<ApiResponse<Void>> cancelEnrolment(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        enrolmentService.cancelEnrolment(courseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Enrolment cancelled successfully"));
    }
}
