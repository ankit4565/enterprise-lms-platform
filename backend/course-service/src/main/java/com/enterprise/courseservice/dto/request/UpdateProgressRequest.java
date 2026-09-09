package com.enterprise.courseservice.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {

    @PositiveOrZero(message = "Last position must be non-negative")
    private Integer lastPositionSeconds;

    @PositiveOrZero(message = "Watched seconds must be non-negative")
    private Integer watchedSeconds;

    private Boolean isCompleted;
}
