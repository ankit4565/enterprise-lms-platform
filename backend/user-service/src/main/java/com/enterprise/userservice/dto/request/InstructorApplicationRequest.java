package com.enterprise.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorApplicationRequest {

    @NotBlank(message = "Qualifications are required")
    private String qualifications;

    @Size(max = 500, message = "Sample URL must be at most 500 characters")
    private String sampleUrl;
}
