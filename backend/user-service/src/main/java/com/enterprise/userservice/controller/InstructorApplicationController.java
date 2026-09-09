package com.enterprise.userservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.userservice.dto.request.InstructorApplicationRequest;
import com.enterprise.userservice.dto.request.ReviewApplicationRequest;
import com.enterprise.userservice.dto.response.InstructorApplicationResponse;
import com.enterprise.userservice.entity.ApplicationStatus;
import com.enterprise.userservice.security.UserPrincipal;
import com.enterprise.userservice.service.InstructorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/instructor-applications")
@RequiredArgsConstructor
@Tag(name = "Instructor Applications", description = "Instructor onboarding and application management")
public class InstructorApplicationController {

    private final InstructorApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Submit an application to become an instructor")
    public ResponseEntity<ApiResponse<InstructorApplicationResponse>> submitApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InstructorApplicationRequest request
    ) {
        InstructorApplicationResponse response = applicationService.submitApplication(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Instructor application submitted successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user's instructor applications")
    public ResponseEntity<ApiResponse<List<InstructorApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<InstructorApplicationResponse> applications = applicationService.getMyApplications(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(applications, "Instructor applications retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List instructor applications (Admin only)")
    public ResponseEntity<ApiResponse<Page<InstructorApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<InstructorApplicationResponse> page = applicationService.getAllApplications(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page, "Instructor applications retrieved successfully"));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Review and approve/reject an instructor application (Admin only)")
    public ResponseEntity<ApiResponse<InstructorApplicationResponse>> reviewApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewApplicationRequest request
    ) {
        InstructorApplicationResponse reviewed = applicationService.reviewApplication(id, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed, "Instructor application reviewed successfully"));
    }
}
