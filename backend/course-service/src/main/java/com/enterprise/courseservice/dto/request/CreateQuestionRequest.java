package com.enterprise.courseservice.dto.request;

import com.enterprise.courseservice.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotNull(message = "Question type is required")
    private QuestionType type;

    @NotBlank(message = "Question text is required")
    private String text;

    @DecimalMin(value = "0.0", message = "Marks must be non-negative")
    @Builder.Default
    private BigDecimal marks = BigDecimal.ONE;

    @DecimalMin(value = "0.0", message = "Negative marks must be non-negative")
    @Builder.Default
    private BigDecimal negativeMarks = BigDecimal.ZERO;

    private String explanation;

    @Builder.Default
    private Integer position = 0;

    // For SHORT_ANSWER
    private String correctText;

    // For NUMERIC
    private BigDecimal numericAnswer;

    @DecimalMin(value = "0.0", message = "Tolerance must be non-negative")
    @Builder.Default
    private BigDecimal tolerance = BigDecimal.ZERO;

    // For SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE
    @Valid
    @Builder.Default
    private List<CreateOptionRequest> options = new ArrayList<>();
}
