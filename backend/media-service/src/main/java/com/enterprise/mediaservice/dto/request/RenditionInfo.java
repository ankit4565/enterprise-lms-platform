package com.enterprise.mediaservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenditionInfo {
    private String resolution;
    private String url;
    private Integer bitrate;
    private Integer width;
    private Integer height;
}
