package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAssignmentRequest {

    @NotBlank(message = "Assignment title is required")
    @Size(max = 200, message = "Assignment title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Instructions are required")
    private String instructions;

    @Min(value = 1, message = "Max score must be at least 1")
    @Builder.Default
    private Integer maxScore = 100;

    private Instant dueAt;

    @Builder.Default
    private Boolean allowLate = true;

    @Min(value = 0, message = "Late penalty percent must be at least 0")
    @Max(value = 100, message = "Late penalty percent cannot exceed 100")
    @Builder.Default
    private Integer latePenaltyPercent = 0;

    private String allowedFileTypes;

    @Min(value = 1, message = "Max file size must be at least 1 MB")
    @Max(value = 500, message = "Max file size cannot exceed 500 MB")
    @Builder.Default
    private Integer maxFileSizeMb = 50;
}
