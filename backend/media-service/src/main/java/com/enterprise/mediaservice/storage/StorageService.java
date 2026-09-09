package com.enterprise.mediaservice.storage;

import com.enterprise.mediaservice.dto.request.PartInfo;

import java.time.Duration;
import java.util.List;

public interface StorageService {

    String initiateMultipartUpload(String key, String contentType);

    String generatePresignedPartUploadUrl(String key, String uploadId, int partNumber, Duration expiry);

    String generatePresignedPutUrl(String key, String contentType, Duration expiry);

    void completeMultipartUpload(String key, String uploadId, List<PartInfo> parts);

    void abortMultipartUpload(String key, String uploadId);

    String generatePresignedGetUrl(String key, Duration expiry);

    void deleteObject(String key);
}
