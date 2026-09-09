package com.enterprise.courseservice.dto.response;

import com.enterprise.courseservice.entity.QuizOption;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOptionAdminResponse {

    private UUID id;
    private UUID questionId;
    private String text;
    private Boolean isCorrect;
    private Integer position;

    public static QuizOptionAdminResponse from(QuizOption option) {
        return QuizOptionAdminResponse.builder()
                .id(option.getId())
                .questionId(option.getQuestion() != null ? option.getQuestion().getId() : null)
                .text(option.getText())
                .isCorrect(option.getIsCorrect())
                .position(option.getPosition())
                .build();
    }
}
