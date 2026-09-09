package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.LessonResponse;
import com.enterprise.courseservice.dto.response.ModuleResponse;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.ModuleLessonService;
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
@RequestMapping("/api/v1/courses/{courseId}/modules")
@RequiredArgsConstructor
@Tag(name = "Modules & Lessons", description = "Curriculum builder for course modules and lessons")
public class ModuleLessonController {

    private final ModuleLessonService moduleLessonService;

    // --- Module Endpoints ---

    @GetMapping
    @Operation(summary = "Get all modules for a course with lessons")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(@PathVariable UUID courseId) {
        List<ModuleResponse> modules = moduleLessonService.getModulesByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok(modules, "Modules retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add a new module to a course")
    public ResponseEntity<ApiResponse<ModuleResponse>> addModule(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateModuleRequest request
    ) {
        ModuleResponse module = moduleLessonService.addModule(
                courseId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(module, "Module added successfully"));
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update module details")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateModuleRequest request
    ) {
        ModuleResponse updated = moduleLessonService.updateModule(
                courseId, moduleId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(updated, "Module updated successfully"));
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete module and its lessons")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        moduleLessonService.deleteModule(courseId, moduleId, principal.getUserId(), principal.getRoles());
        return ResponseEntity.ok(ApiResponse.ok(null, "Module deleted successfully"));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Bulk reorder modules in a course")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> reorderModules(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderPositionsRequest request
    ) {
        List<ModuleResponse> reordered = moduleLessonService.reorderModules(
                courseId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(reordered, "Modules reordered successfully"));
    }

    // --- Lesson Endpoints ---

    @PostMapping("/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add a new lesson to a module")
    public ResponseEntity<ApiResponse<LessonResponse>> addLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateLessonRequest request
    ) {
        LessonResponse lesson = moduleLessonService.addLesson(
                courseId, moduleId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(lesson, "Lesson added successfully"));
    }

    @PutMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update lesson details")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateLessonRequest request
    ) {
        LessonResponse updated = moduleLessonService.updateLesson(
                courseId, moduleId, lessonId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(updated, "Lesson updated successfully"));
    }

    @DeleteMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete lesson")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        moduleLessonService.deleteLesson(courseId, moduleId, lessonId, principal.getUserId(), principal.getRoles());
        return ResponseEntity.ok(ApiResponse.ok(null, "Lesson deleted successfully"));
    }

    @PutMapping("/{moduleId}/lessons/reorder")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Bulk reorder lessons in a module")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> reorderLessons(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderPositionsRequest request
    ) {
        List<LessonResponse> reordered = moduleLessonService.reorderLessons(
                courseId, moduleId, principal.getUserId(), principal.getRoles(), request
        );
        return ResponseEntity.ok(ApiResponse.ok(reordered, "Lessons reordered successfully"));
    }
}
