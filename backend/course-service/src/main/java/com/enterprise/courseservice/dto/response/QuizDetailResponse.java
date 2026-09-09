package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.Quiz;
import com.enterprise.courseservice.entity.ShowAnswersPolicy;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizDetailResponse {

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
    private List<QuizQuestionAdminResponse> questions;
    private Instant createdAt;
    private Instant updatedAt;

    public static QuizDetailResponse from(Quiz quiz) {
        List<QuizQuestionAdminResponse> questionList = quiz.getQuestions() != null
                ? quiz.getQuestions().stream().map(QuizQuestionAdminResponse::from).toList()
                : Collections.emptyList();

        return QuizDetailResponse.builder()
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
                .questions(questionList)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .build();
    }
}
