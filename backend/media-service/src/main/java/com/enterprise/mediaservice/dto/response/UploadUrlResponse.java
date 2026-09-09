package com.enterprise.mediaservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
    private UUID mediaId;
    private String uploadId;
    private String storageKey;
    private Long totalSizeBytes;
    private Integer totalParts;
    private List<PartUploadUrl> partUrls;
    private Long expiresInSeconds;
}
