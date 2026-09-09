package com.enterprise.mediaservice.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "media")
public class MediaProperties {
    private Limits limits = new Limits();
    private long presignedExpirySeconds = 86400L;
    private long streamExpirySeconds = 1800L;

    @Data
    public static class Limits {
        private long videoMaxBytes = 2147483648L;      // 2 GB
        private long documentMaxBytes = 52428800L;    // 50 MB
        private long imageMaxBytes = 5242880L;        // 5 MB
        private long defaultPartSize = 10485760L;     // 10 MB
    }
}
