package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.AssignmentSubmission;
import com.enterprise.courseservice.entity.SubmissionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {

    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private Integer attemptNo;
    private String textAnswer;
    private String fileUrls;
    private SubmissionStatus status;
    private Boolean isLate;
    private BigDecimal rawScore;
    private BigDecimal finalScore;
    private String feedback;
    private UUID gradedBy;
    private Instant gradedAt;
    private Instant submittedAt;

    public static SubmissionResponse from(AssignmentSubmission sub) {
        if (sub == null) return null;
        return SubmissionResponse.builder()
                .id(sub.getId())
                .assignmentId(sub.getAssignment() != null ? sub.getAssignment().getId() : null)
                .studentId(sub.getStudentId())
                .attemptNo(sub.getAttemptNo())
                .textAnswer(sub.getTextAnswer())
                .fileUrls(sub.getFileUrls())
                .status(sub.getStatus())
                .isLate(sub.getIsLate())
                .rawScore(sub.getRawScore())
                .finalScore(sub.getFinalScore())
                .feedback(sub.getFeedback())
                .gradedBy(sub.getGradedBy())
                .gradedAt(sub.getGradedAt())
                .submittedAt(sub.getSubmittedAt())
                .build();
    }
}
