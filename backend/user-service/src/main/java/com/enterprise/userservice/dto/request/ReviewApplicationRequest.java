package com.enterprise.userservice.dto.request;

import com.enterprise.userservice.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewApplicationRequest {

    @NotNull(message = "Status must be APPROVED or REJECTED")
    private ApplicationStatus status;

    private String reviewNote;
}
