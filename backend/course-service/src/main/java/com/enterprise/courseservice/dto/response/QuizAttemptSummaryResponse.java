package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.AttemptStatus;
import com.enterprise.courseservice.entity.QuizAttempt;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptSummaryResponse {

    private UUID attemptId;
    private UUID quizId;
    private Integer attemptNo;
    private Instant startedAt;
    private Instant submittedAt;
    private AttemptStatus status;
    private BigDecimal score;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private Boolean passed;
    private Integer timeTakenSeconds;

    public static QuizAttemptSummaryResponse from(QuizAttempt attempt) {
        return QuizAttemptSummaryResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz() != null ? attempt.getQuiz().getId() : null)
                .attemptNo(attempt.getAttemptNo())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .status(attempt.getStatus())
                .score(attempt.getScore())
                .totalMarks(attempt.getQuiz() != null ? attempt.getQuiz().getTotalMarks() : null)
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .build();
    }
}
