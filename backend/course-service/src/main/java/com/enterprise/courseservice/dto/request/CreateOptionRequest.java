package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOptionRequest {

    @NotBlank(message = "Option text is required")
    @Size(max = 500, message = "Option text cannot exceed 500 characters")
    private String text;

    @NotNull(message = "isCorrect flag is required")
    @Builder.Default
    private Boolean isCorrect = false;

    @Builder.Default
    private Integer position = 0;
}
