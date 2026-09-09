package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.courseservice.dto.request.UpdateProgressRequest;
import com.enterprise.courseservice.dto.response.CourseProgressResponse;
import com.enterprise.courseservice.dto.response.LessonProgressResponse;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.LessonProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Student lesson progress tracking and course completion")
public class ProgressController {

    private final LessonProgressService lessonProgressService;

    @GetMapping("/progress")
    @Operation(summary = "Get overall course progress for current student")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getCourseProgress(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CourseProgressResponse progress = lessonProgressService.getCourseProgress(courseId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(progress, "Course progress retrieved successfully"));
    }

    @PostMapping("/lessons/{lessonId}/progress")
    @Operation(summary = "Update progress for a specific lesson")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateLessonProgress(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        LessonProgressResponse response = lessonProgressService.recordProgress(
                courseId, lessonId, principal.getUserId(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Lesson progress updated successfully"));
    }
}
