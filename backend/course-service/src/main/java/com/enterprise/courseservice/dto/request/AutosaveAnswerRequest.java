package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutosaveAnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    // For SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE
    private List<UUID> selectedOptionIds;

    // For SHORT_ANSWER
    private String textAnswer;

    // For NUMERIC
    private BigDecimal numericAnswer;
}
