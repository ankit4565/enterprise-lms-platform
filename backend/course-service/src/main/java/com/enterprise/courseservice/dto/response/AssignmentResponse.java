package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Assignment;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {

    private UUID id;
    private UUID lessonId;
    private UUID courseId;
    private String title;
    private String instructions;
    private Integer maxScore;
    private Instant dueAt;
    private Boolean allowLate;
    private Integer latePenaltyPercent;
    private String allowedFileTypes;
    private Integer maxFileSizeMb;
    private Instant createdAt;
    private Instant updatedAt;

    public static AssignmentResponse from(Assignment assignment) {
        if (assignment == null) return null;
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .lessonId(assignment.getLesson() != null ? assignment.getLesson().getId() : null)
                .courseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null)
                .title(assignment.getTitle())
                .instructions(assignment.getInstructions())
                .maxScore(assignment.getMaxScore())
                .dueAt(assignment.getDueAt())
                .allowLate(assignment.getAllowLate())
                .latePenaltyPercent(assignment.getLatePenaltyPercent())
                .allowedFileTypes(assignment.getAllowedFileTypes())
                .maxFileSizeMb(assignment.getMaxFileSizeMb())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
}
