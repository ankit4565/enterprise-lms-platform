package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.AttemptStatus;
import com.enterprise.courseservice.entity.QuestionType;
import com.enterprise.courseservice.entity.ShowAnswersPolicy;
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
public class QuizResultResponse {

    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private UUID studentId;
    private Integer attemptNo;
    private Instant startedAt;
    private Instant submittedAt;
    private AttemptStatus status;
    private BigDecimal score;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private Boolean passed;
    private BigDecimal passPercent;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer unansweredCount;
    private Integer timeTakenSeconds;
    private ShowAnswersPolicy showAnswersPolicy;
    private List<QuestionResultDto> questionResults;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionResultDto {
        private UUID questionId;
        private QuestionType type;
        private String text;
        private BigDecimal marks;
        private BigDecimal negativeMarks;
        private Boolean isCorrect;
        private BigDecimal marksAwarded;
        private List<UUID> studentSelectedOptionIds;
        private String studentTextAnswer;
        private BigDecimal studentNumericAnswer;

        // Conditionally populated if policy allows or instructor
        private List<UUID> correctOptionIds;
        private String correctText;
        private BigDecimal numericAnswer;
        private BigDecimal tolerance;
        private String explanation;
        private List<OptionResultDto> options;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionResultDto {
        private UUID id;
        private String text;
        private Boolean isCorrect; // revealed if policy allows or instructor
    }
}
