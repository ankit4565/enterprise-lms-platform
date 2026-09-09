package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.ShowAnswersPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuizRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @Min(value = 1, message = "Time limit must be at least 1 minute if specified")
    private Integer timeLimitMinutes;

    @Min(value = 0, message = "Max attempts must be 0 (unlimited) or greater")
    private Integer maxAttempts;

    @DecimalMin(value = "0.0", message = "Pass percent must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Pass percent must be between 0 and 100")
    private BigDecimal passPercent;

    private Boolean shuffleQuestions;

    private Boolean shuffleOptions;

    private ShowAnswersPolicy showAnswersPolicy;

    private Boolean isPublished;
}
