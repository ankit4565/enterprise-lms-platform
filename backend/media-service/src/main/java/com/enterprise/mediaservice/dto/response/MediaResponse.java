package com.enterprise.mediaservice.dto.response;

import com.enterprise.mediaservice.entity.MediaContextType;
import com.enterprise.mediaservice.entity.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private UUID id;
    private UUID ownerId;
    private MediaContextType contextType;
    private UUID contextId;
    private String originalFilename;
    private String mimeType;
    private Long sizeBytes;
    private String storageKey;
    private String cdnUrl;
    private String hlsManifestUrl;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private MediaStatus status;
    private String checksumSha256;
    private Instant createdAt;
    private Instant updatedAt;
}
