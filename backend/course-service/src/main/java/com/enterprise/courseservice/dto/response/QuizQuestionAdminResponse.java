package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.QuestionType;
import com.enterprise.courseservice.entity.QuizQuestion;
import lombok.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionAdminResponse {

    private UUID id;
    private UUID quizId;
    private QuestionType type;
    private String text;
    private BigDecimal marks;
    private BigDecimal negativeMarks;
    private String explanation;
    private Integer position;
    private String correctText;
    private BigDecimal numericAnswer;
    private BigDecimal tolerance;
    private List<QuizOptionAdminResponse> options;

    public static QuizQuestionAdminResponse from(QuizQuestion question) {
        List<QuizOptionAdminResponse> optionList = question.getOptions() != null
                ? question.getOptions().stream().map(QuizOptionAdminResponse::from).toList()
                : Collections.emptyList();

        return QuizQuestionAdminResponse.builder()
                .id(question.getId())
                .quizId(question.getQuiz() != null ? question.getQuiz().getId() : null)
                .type(question.getType())
                .text(question.getText())
                .marks(question.getMarks())
                .negativeMarks(question.getNegativeMarks())
                .explanation(question.getExplanation())
                .position(question.getPosition())
                .correctText(question.getCorrectText())
                .numericAnswer(question.getNumericAnswer())
                .tolerance(question.getTolerance())
                .options(optionList)
                .build();
    }
}
