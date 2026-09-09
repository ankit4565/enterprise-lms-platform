package com.enterprise.mediaservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartInfo {

    @NotNull(message = "Part number is required")
    @Min(value = 1, message = "Part number must be >= 1")
    private Integer partNumber;

    @NotBlank(message = "ETag is required")
    @JsonProperty("eTag")
    private String eTag;
}
