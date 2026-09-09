package com.enterprise.mediaservice.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.s3")
public class StorageProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "enterprise-lms-media";
    private String region = "us-east-1";
    private boolean pathStyleAccess = true;
    private String cdnBaseUrl = "http://localhost:9000/enterprise-lms-media";
}
