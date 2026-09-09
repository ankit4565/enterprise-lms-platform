package com.enterprise.mediaservice.dto.request;

import com.enterprise.mediaservice.entity.TranscodeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeCallbackRequest {

    @NotNull(message = "Status is required")
    private TranscodeStatus status;

    private String hlsManifestUrl;

    private Integer durationSeconds;

    private Integer width;

    private Integer height;

    private List<RenditionInfo> renditions;

    private String error;
}
