package com.enterprise.courseservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.dto.PageResponse;
import com.enterprise.courseservice.dto.request.CreateReviewRequest;
import com.enterprise.courseservice.dto.request.UpdateReviewRequest;
import com.enterprise.courseservice.dto.response.ReviewResponse;
import com.enterprise.courseservice.security.UserPrincipal;
import com.enterprise.courseservice.service.ReviewService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Course ratings, student reviews, and moderation")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Get approved reviews for a course (Public)")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getCourseReviews(
            @PathVariable UUID courseId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReviewResponse> reviews = reviewService.getCourseReviews(courseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(reviews), "Reviews retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Submit a course review (Enrolled Student)")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse review = reviewService.createReview(courseId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(review, "Review submitted successfully"));
    }

    @PutMapping
    @Operation(summary = "Update your existing review (Student)")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        ReviewResponse updated = reviewService.updateReview(courseId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Review updated successfully"));
    }

    @DeleteMapping
    @Operation(summary = "Delete a course review (Owner/Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(courseId, principal.getUserId(), principal.getRoles());
        return ResponseEntity.ok(ApiResponse.ok(null, "Review deleted successfully"));
    }
}
