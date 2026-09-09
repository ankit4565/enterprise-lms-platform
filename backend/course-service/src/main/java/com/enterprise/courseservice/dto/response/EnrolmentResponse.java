package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Enrolment;
import com.enterprise.courseservice.entity.EnrolmentSource;
import com.enterprise.courseservice.entity.EnrolmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrolmentResponse {

    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String courseSlug;
    private String courseThumbnailUrl;
    private UUID studentId;
    private EnrolmentSource source;
    private UUID orderId;
    private EnrolmentStatus status;
    private BigDecimal progressPercent;
    private Instant enrolledAt;
    private Instant completedAt;
    private Instant lastAccessedAt;

    public static EnrolmentResponse fromEntity(Enrolment enrolment) {
        if (enrolment == null) return null;
        return EnrolmentResponse.builder()
                .id(enrolment.getId())
                .courseId(enrolment.getCourse() != null ? enrolment.getCourse().getId() : null)
                .courseTitle(enrolment.getCourse() != null ? enrolment.getCourse().getTitle() : null)
                .courseSlug(enrolment.getCourse() != null ? enrolment.getCourse().getSlug() : null)
                .courseThumbnailUrl(enrolment.getCourse() != null ? enrolment.getCourse().getThumbnailUrl() : null)
                .studentId(enrolment.getStudentId())
                .source(enrolment.getSource())
                .orderId(enrolment.getOrderId())
                .status(enrolment.getStatus())
                .progressPercent(enrolment.getProgressPercent())
                .enrolledAt(enrolment.getEnrolledAt())
                .completedAt(enrolment.getCompletedAt())
                .lastAccessedAt(enrolment.getLastAccessedAt())
                .build();
    }
}
