package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.courseservice.dto.request.ReviewCourseRequest;
import com.enterprise.courseservice.dto.response.CourseDetailResponse;
import com.enterprise.courseservice.dto.response.CourseSummaryResponse;
import com.enterprise.courseservice.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Courses", description = "Course review, publication moderation, and administration")
public class AdminCourseController {

    private final CourseService courseService;

    @GetMapping("/pending")
    @Operation(summary = "Get courses pending admin review (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> getPendingCourses(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CourseSummaryResponse> courses = courseService.getPendingCourses(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(courses), "Pending courses retrieved successfully"));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Approve or reject a submitted course (Admin only)")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> reviewCourse(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewCourseRequest request
    ) {
        CourseDetailResponse reviewed = courseService.reviewCourse(id, request);
        return ResponseEntity.ok(ApiResponse.ok(reviewed, "Course review completed successfully"));
    }
}
