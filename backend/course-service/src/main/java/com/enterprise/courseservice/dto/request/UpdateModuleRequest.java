package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateModuleRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;
}
