package com.enterprise.mediaservice.dto.request;

import com.enterprise.mediaservice.entity.MediaContextType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUploadUrlRequest {

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "MIME type is required")
    private String mimeType;

    @NotNull(message = "Size in bytes is required")
    @Min(value = 1, message = "Size must be greater than 0")
    private Long sizeBytes;

    @NotNull(message = "Context type is required")
    private MediaContextType contextType;

    private UUID contextId;
}
