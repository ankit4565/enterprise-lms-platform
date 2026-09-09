package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.ShowAnswersPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuizRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @Min(value = 1, message = "Time limit must be at least 1 minute if specified")
    private Integer timeLimitMinutes;

    @Min(value = 0, message = "Max attempts must be 0 (unlimited) or greater")
    @Builder.Default
    private Integer maxAttempts = 1;

    @DecimalMin(value = "0.0", message = "Pass percent must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Pass percent must be between 0 and 100")
    @Builder.Default
    private BigDecimal passPercent = new BigDecimal("60.00");

    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Builder.Default
    private Boolean shuffleOptions = false;

    @Builder.Default
    private ShowAnswersPolicy showAnswersPolicy = ShowAnswersPolicy.AFTER_SUBMIT;

    @Builder.Default
    private Boolean isPublished = true;
}
