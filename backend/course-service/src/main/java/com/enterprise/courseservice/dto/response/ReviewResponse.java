package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID courseId;
    private UUID studentId;
    private Integer rating;
    private String comment;
    private Boolean isApproved;
    private Instant createdAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) return null;
        return ReviewResponse.builder()
                .id(review.getId())
                .courseId(review.getCourse() != null ? review.getCourse().getId() : null)
                .studentId(review.getStudentId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
