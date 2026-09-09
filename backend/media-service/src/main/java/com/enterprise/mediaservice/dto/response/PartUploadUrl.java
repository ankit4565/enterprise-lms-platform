package com.enterprise.mediaservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartUploadUrl {
    private Integer partNumber;
    private String uploadUrl;
    private Long sizeBytes;
}
