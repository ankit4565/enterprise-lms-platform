package com.enterprise.mediaservice.storage;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.mediaservice.dto.request.PartInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    @Override
    public String initiateMultipartUpload(String key, String contentType) {
        try {
            ensureBucketExists();
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
            log.info("Initiated multipart upload for key {} with uploadId {}", key, response.uploadId());
            return response.uploadId();
        } catch (SdkException e) {
            log.error("Failed to initiate multipart upload for key {}: {}", key, e.getMessage());
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to initiate multipart upload: " + e.getMessage());
        }
    }

    @Override
    public String generatePresignedPartUploadUrl(String key, String uploadId, int partNumber, Duration expiry) {
        try {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(expiry)
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            return s3Presigner.presignUploadPart(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("Failed to generate presigned upload part URL for key {} part {}: {}", key, partNumber, e.getMessage());
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate presigned part upload URL: " + e.getMessage());
        }
    }

    @Override
    public String generatePresignedPutUrl(String key, String contentType, Duration expiry) {
        try {
            ensureBucketExists();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .putObjectRequest(putObjectRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("Failed to generate presigned PUT URL for key {}: {}", key, e.getMessage());
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate presigned PUT URL: " + e.getMessage());
        }
    }

    @Override
    public void completeMultipartUpload(String key, String uploadId, List<PartInfo> parts) {
        try {
            List<CompletedPart> completedParts = parts.stream()
                    .map(p -> CompletedPart.builder()
                            .partNumber(p.getPartNumber())
                            .eTag(cleanETag(p.getETag()))
                            .build())
                    .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                    .collect(Collectors.toList());

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            s3Client.completeMultipartUpload(request);
            log.info("Completed multipart upload for key {} uploadId {}", key, uploadId);
        } catch (SdkException e) {
            log.error("Failed to complete multipart upload for key {}: {}", key, e.getMessage());
            throw new BadRequestException("Failed to complete multipart upload: " + e.getMessage());
        }
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(request);
            log.info("Aborted multipart upload for key {} uploadId {}", key, uploadId);
        } catch (SdkException e) {
            log.warn("Failed to abort multipart upload for key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public String generatePresignedGetUrl(String key, Duration expiry) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException e) {
            log.error("Failed to generate presigned GET URL for key {}: {}", key, e.getMessage());
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate presigned GET URL: " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Deleted S3 object at key {}", key);
        } catch (SdkException e) {
            log.warn("Failed to delete S3 object at key {}: {}", key, e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(properties.getBucketName())
                    .build();
            s3Client.headBucket(headRequest);
        } catch (NoSuchBucketException e) {
            log.info("Bucket {} does not exist, creating it...", properties.getBucketName());
            try {
                CreateBucketRequest createRequest = CreateBucketRequest.builder()
                        .bucket(properties.getBucketName())
                        .build();
                s3Client.createBucket(createRequest);
                log.info("Bucket {} created successfully", properties.getBucketName());
            } catch (Exception ex) {
                log.warn("Could not create bucket {}: {}", properties.getBucketName(), ex.getMessage());
            }
        } catch (Exception e) {
            log.debug("HeadBucket check result for {}: {}", properties.getBucketName(), e.getMessage());
        }
    }

    private String cleanETag(String eTag) {
        if (eTag == null) return "";
        String trimmed = eTag.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
