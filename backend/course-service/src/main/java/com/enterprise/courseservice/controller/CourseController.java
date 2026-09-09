package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.courseservice.dto.request.CreateCourseRequest;
import com.enterprise.courseservice.dto.request.UpdateCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.dto.response.CourseSummaryResponse;
import com.enterprise.courseservice.entity.CourseLevel;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course creation, catalogue browsing, and lifecycle management")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Search and filter course catalogue (Public)")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> searchCatalogue(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CourseSummaryResponse> results = courseService.searchCatalogue(
                q, categoryId, level, language, minPrice, maxPrice, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(results), "Catalogue retrieved successfully"));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Get course details by slug or UUID (Public)")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @PathVariable String slugOrId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID requesterId = principal != null ? principal.getUserId() : null;
        List<String> requesterRoles = principal != null ? principal.getRoles() : null;

        CourseDetailResponse course = courseService.getCourseDetail(slugOrId, requesterId, requesterRoles);
        return ResponseEntity.ok(ApiResponse.ok(course, "Course detail retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new course (Instructor/Admin)")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> createCourse(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        CourseDetailResponse created = courseService.createCourse(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Course created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update course details (Owner/Admin)")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> updateCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        CourseDetailResponse updated = courseService.updateCourse(
                id, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(updated, "Course updated successfully"));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Submit course for review & publishing (Owner/Admin)")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> submitCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CourseDetailResponse submitted = courseService.submitCourseForReview(
                id, principal.getUserId(), principal.getRoles()
        );
        return ResponseEntity.ok(ApiResponse.ok(submitted, "Course submitted for review successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Archive course (Owner/Admin)")
    public ResponseEntity<ApiResponse<Void>> archiveCourse(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        courseService.archiveCourse(id, principal.getUserId(), principal.getRoles());
        return ResponseEntity.ok(ApiResponse.ok(null, "Course archived successfully"));
    }

    @GetMapping("/instructor/my-courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get courses authored by the current instructor")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> getMyCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CourseSummaryResponse> courses = courseService.getInstructorCourses(principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(courses), "Instructor courses retrieved successfully"));
    }
}
