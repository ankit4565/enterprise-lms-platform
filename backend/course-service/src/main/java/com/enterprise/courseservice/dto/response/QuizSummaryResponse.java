package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Quiz;
import com.enterprise.courseservice.entity.ShowAnswersPolicy;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSummaryResponse {

    private UUID id;
    private UUID lessonId;
    private UUID courseId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private BigDecimal passPercent;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private ShowAnswersPolicy showAnswersPolicy;
    private BigDecimal totalMarks;
    private Boolean isPublished;
    private Integer questionsCount;
    private Instant createdAt;
    private Instant updatedAt;

    public static QuizSummaryResponse from(Quiz quiz) {
        int count = quiz.getQuestions() != null ? quiz.getQuestions().size() : 0;
        return from(quiz, count);
    }

    public static QuizSummaryResponse from(Quiz quiz, int questionsCount) {
        return QuizSummaryResponse.builder()
                .id(quiz.getId())
                .lessonId(quiz.getLesson() != null ? quiz.getLesson().getId() : null)
                .courseId(quiz.getCourse() != null ? quiz.getCourse().getId() : null)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .maxAttempts(quiz.getMaxAttempts())
                .passPercent(quiz.getPassPercent())
                .shuffleQuestions(quiz.getShuffleQuestions())
                .shuffleOptions(quiz.getShuffleOptions())
                .showAnswersPolicy(quiz.getShowAnswersPolicy())
                .totalMarks(quiz.getTotalMarks())
                .isPublished(quiz.getIsPublished())
                .questionsCount(questionsCount)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}
