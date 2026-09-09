package com.enterprise.courseservice.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitQuizRequest {

    // Optional list of final answers to batch save upon submission
    @Valid
    private List<AutosaveAnswerRequest> answers;
}
