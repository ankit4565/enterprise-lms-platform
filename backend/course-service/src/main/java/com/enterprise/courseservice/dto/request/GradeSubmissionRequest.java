package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.SubmissionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeSubmissionRequest {

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score cannot be negative")
    private BigDecimal score;

    private String feedback;

    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.GRADED;
}
