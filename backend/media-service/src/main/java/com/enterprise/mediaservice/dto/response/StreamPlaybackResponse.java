package com.enterprise.mediaservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamPlaybackResponse {
    private UUID mediaId;
    private String streamUrl;
    private String mimeType;
    private Integer durationSeconds;
    private Long expiresInSeconds;
}
