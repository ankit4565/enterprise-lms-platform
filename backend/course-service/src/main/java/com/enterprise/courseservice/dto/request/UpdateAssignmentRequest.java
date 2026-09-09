package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAssignmentRequest {

    @Size(max = 200, message = "Assignment title must not exceed 200 characters")
    private String title;

    private String instructions;

    @Min(value = 1, message = "Max score must be at least 1")
    private Integer maxScore;

    private Instant dueAt;

    private Boolean allowLate;

    @Min(value = 0, message = "Late penalty percent must be at least 0")
    @Max(value = 100, message = "Late penalty percent cannot exceed 100")
    private Integer latePenaltyPercent;

    private String allowedFileTypes;

    @Min(value = 1, message = "Max file size must be at least 1 MB")
    @Max(value = 500, message = "Max file size cannot exceed 500 MB")
    private Integer maxFileSizeMb;
}
