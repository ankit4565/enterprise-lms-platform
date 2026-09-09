package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.QuestionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveQuizAttemptResponse {

    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private Integer attemptNo;
    private Instant startedAt;
    private Instant expiresAt;
    private Integer timeLimitMinutes;
    private BigDecimal totalMarks;
    private List<ActiveQuestionDto> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveQuestionDto {
        private UUID id;
        private QuestionType type;
        private String text;
        private BigDecimal marks;
        private BigDecimal negativeMarks;
        private Integer position;
        private List<ActiveOptionDto> options;
        private SavedAnswerDto savedAnswer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActiveOptionDto {
        private UUID id;
        private String text;
        private Integer position;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SavedAnswerDto {
        private List<UUID> selectedOptionIds;
        private String textAnswer;
        private BigDecimal numericAnswer;
    }
}
