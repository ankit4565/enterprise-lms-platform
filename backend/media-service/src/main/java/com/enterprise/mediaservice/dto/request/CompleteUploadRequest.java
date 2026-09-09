package com.enterprise.mediaservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUploadRequest {

    @NotBlank(message = "Upload ID is required")
    private String uploadId;

    @NotEmpty(message = "Parts list cannot be empty")
    private List<@Valid PartInfo> parts;

    private String checksumSha256;
}
