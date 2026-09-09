package com.enterprise.userservice.dto.response;

import com.enterprise.userservice.entity.ApplicationStatus;
import com.enterprise.userservice.entity.InstructorApplication;
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
public class InstructorApplicationResponse {

    private UUID id;
    private UUID userId;
    private ApplicationStatus status;
    private String qualifications;
    private String sampleUrl;
    private UUID reviewedBy;
    private String reviewNote;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static InstructorApplicationResponse fromEntity(InstructorApplication app) {
        if (app == null) return null;
        return InstructorApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .status(app.getStatus())
                .qualifications(app.getQualifications())
                .sampleUrl(app.getSampleUrl())
                .reviewedBy(app.getReviewedBy())
                .reviewNote(app.getReviewNote())
                .reviewedAt(app.getReviewedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
