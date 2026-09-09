package com.enterprise.mediaservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.mediaservice.dto.request.CompleteUploadRequest;
import com.enterprise.mediaservice.dto.request.CreateUploadUrlRequest;
import com.enterprise.mediaservice.dto.response.MediaResponse;
import com.enterprise.mediaservice.dto.response.PartUploadUrl;
import com.enterprise.mediaservice.dto.response.StreamPlaybackResponse;
import com.enterprise.mediaservice.dto.response.UploadUrlResponse;
import com.enterprise.mediaservice.entity.*;
import com.enterprise.mediaservice.repository.MediaRepository;
import com.enterprise.mediaservice.repository.UploadSessionRepository;
import com.enterprise.mediaservice.storage.MediaProperties;
import com.enterprise.mediaservice.storage.StorageProperties;
import com.enterprise.mediaservice.storage.StorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final StorageService storageService;
    private final TranscodeService transcodeService;
    private final MediaProperties mediaProperties;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_VIDEO_MIMES = Set.of(
            "video/mp4", "video/quicktime", "video/x-matroska", "video/webm", "video/mpeg"
    );

    private static final Set<String> ALLOWED_IMAGE_MIMES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_DOC_MIMES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain"
    );

    @Transactional
    public UploadUrlResponse createUploadUrl(UUID ownerId, CreateUploadUrlRequest request) {
        validateMediaConstraints(request);

        UUID mediaId = UUID.randomUUID();
        String safeFilename = sanitizeFilename(request.getFilename());
        String storageKey = String.format("uploads/%s/%s/%s", ownerId, mediaId, safeFilename);

        long partSize = mediaProperties.getLimits().getDefaultPartSize();
        long sizeBytes = request.getSizeBytes();
        int totalParts = (int) Math.ceil((double) sizeBytes / partSize);
        if (totalParts == 0) {
            totalParts = 1;
        }

        Duration expiry = Duration.ofSeconds(mediaProperties.getPresignedExpirySeconds());
        String uploadId = storageService.initiateMultipartUpload(storageKey, request.getMimeType());

        List<PartUploadUrl> partUrls = new ArrayList<>(totalParts);
        for (int i = 1; i <= totalParts; i++) {
            long currentPartSize = (i == totalParts) ? (sizeBytes - (long) (i - 1) * partSize) : partSize;
            String partUrl = storageService.generatePresignedPartUploadUrl(storageKey, uploadId, i, expiry);
            partUrls.add(new PartUploadUrl(i, partUrl, currentPartSize));
        }

        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .contextType(request.getContextType())
                .contextId(request.getContextId())
                .originalFilename(request.getFilename())
                .mimeType(request.getMimeType().toLowerCase())
                .sizeBytes(request.getSizeBytes())
                .storageKey(storageKey)
                .status(MediaStatus.PENDING)
                .build();

        media = mediaRepository.save(media);

        String partsJson;
        try {
            partsJson = objectMapper.writeValueAsString(partUrls);
        } catch (JsonProcessingException e) {
            partsJson = "[]";
        }

        UploadSession session = UploadSession.builder()
                .media(media)
                .uploadId(uploadId)
                .parts(partsJson)
                .expiresAt(Instant.now().plus(expiry))
                .build();

        uploadSessionRepository.save(session);
        log.info("Created upload session for media {} owner {} with {} parts", mediaId, ownerId, totalParts);

        return UploadUrlResponse.builder()
                .mediaId(mediaId)
                .uploadId(uploadId)
                .storageKey(storageKey)
                .totalSizeBytes(sizeBytes)
                .totalParts(totalParts)
                .partUrls(partUrls)
                .expiresInSeconds(mediaProperties.getPresignedExpirySeconds())
                .build();
    }

    @Transactional
    public MediaResponse completeUpload(UUID userId, UUID mediaId, CompleteUploadRequest request, boolean isAdmin) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        if (!isAdmin && !media.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You do not own this media resource");
        }

        if (media.getStatus() != MediaStatus.PENDING) {
            throw new BadRequestException("Media is already finalized with status: " + media.getStatus());
        }

        UploadSession session = uploadSessionRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("UploadSession", "mediaId", mediaId));

        if (!session.getUploadId().equals(request.getUploadId())) {
            throw new BadRequestException("Provided uploadId does not match session");
        }

        if (Instant.now().isAfter(session.getExpiresAt())) {
            throw new BadRequestException("The upload session has expired");
        }

        if (session.getCompletedAt() != null) {
            throw new BadRequestException("This upload session is already completed");
        }

        // Finalize S3 multipart upload
        storageService.completeMultipartUpload(media.getStorageKey(), request.getUploadId(), request.getParts());
        session.setCompletedAt(Instant.now());
        uploadSessionRepository.save(session);

        if (request.getChecksumSha256() != null && !request.getChecksumSha256().isBlank()) {
            media.setChecksumSha256(request.getChecksumSha256());
        }

        if (media.getContextType().isVideo()) {
            media.setStatus(MediaStatus.PROCESSING);
            mediaRepository.save(media);

            // Queue and process transcoding job
            TranscodeJob job = transcodeService.createAndQueueJob(media);
            transcodeService.processTranscodeAsync(media.getId(), job.getId());
            log.info("Media {} moved to PROCESSING and transcode job {} queued", mediaId, job.getId());
        } else {
            media.setStatus(MediaStatus.READY);
            media.setCdnUrl(String.format("%s/%s", storageProperties.getCdnBaseUrl(), media.getStorageKey()));
            mediaRepository.save(media);
            log.info("Media {} non-video finalized to READY", mediaId);
        }

        return toMediaResponse(media);
    }

    @Transactional(readOnly = true)
    public MediaResponse getMedia(UUID userId, UUID mediaId, boolean isAdmin) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        return toMediaResponse(media);
    }

    @Transactional(readOnly = true)
    public StreamPlaybackResponse getStreamPlayback(UUID userId, UUID mediaId, boolean isAdmin) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        if (media.getStatus() != MediaStatus.READY) {
            throw new BadRequestException("Media is not ready for playback. Current status: " + media.getStatus());
        }

        long streamExpirySeconds = mediaProperties.getStreamExpirySeconds();
        Duration expiry = Duration.ofSeconds(streamExpirySeconds);

        String streamUrl;
        if (media.getHlsManifestUrl() != null && !media.getHlsManifestUrl().isBlank()) {
            // Adaptive HLS playlist stream
            streamUrl = media.getHlsManifestUrl();
        } else {
            // Presigned direct playback URL
            streamUrl = storageService.generatePresignedGetUrl(media.getStorageKey(), expiry);
        }

        return StreamPlaybackResponse.builder()
                .mediaId(media.getId())
                .streamUrl(streamUrl)
                .mimeType(media.getMimeType())
                .durationSeconds(media.getDurationSeconds())
                .expiresInSeconds(streamExpirySeconds)
                .build();
    }

    @Transactional
    public void deleteMedia(UUID userId, UUID mediaId, boolean isAdmin) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        if (!isAdmin && !media.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You do not have permission to delete this media");
        }

        media.setDeleted(true);
        media.setDeletedAt(Instant.now());
        mediaRepository.save(media);

        // Schedule S3 object deletion
        storageService.deleteObject(media.getStorageKey());
        log.info("Media {} soft deleted by user {}", mediaId, userId);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> listMediaByOwner(UUID ownerId) {
        return mediaRepository.findByOwnerIdAndIsDeletedFalse(ownerId).stream()
                .map(this::toMediaResponse)
                .collect(Collectors.toList());
    }

    private void validateMediaConstraints(CreateUploadUrlRequest request) {
        String mime = request.getMimeType().toLowerCase().trim();
        long size = request.getSizeBytes();
        MediaContextType context = request.getContextType();

        if (context.isVideo()) {
            if (!ALLOWED_VIDEO_MIMES.contains(mime)) {
                throw new BadRequestException("Invalid video format: " + mime + ". Allowed: MP4, MOV, MKV, WebM");
            }
            if (size > mediaProperties.getLimits().getVideoMaxBytes()) {
                throw new BadRequestException("Video size exceeds limit of 2 GB");
            }
        } else if (context == MediaContextType.THUMBNAIL || context == MediaContextType.AVATAR) {
            if (!ALLOWED_IMAGE_MIMES.contains(mime)) {
                throw new BadRequestException("Invalid image format: " + mime + ". Allowed: JPEG, PNG, WEBP, GIF");
            }
            if (size > mediaProperties.getLimits().getImageMaxBytes()) {
                throw new BadRequestException("Image size exceeds limit of 5 MB");
            }
        } else {
            // ASSIGNMENT / CHAT_ATTACHMENT
            boolean isAllowedDoc = ALLOWED_DOC_MIMES.contains(mime) || ALLOWED_IMAGE_MIMES.contains(mime) || ALLOWED_VIDEO_MIMES.contains(mime);
            if (!isAllowedDoc) {
                throw new BadRequestException("Unsupported document format: " + mime);
            }
            if (size > mediaProperties.getLimits().getDocumentMaxBytes()) {
                throw new BadRequestException("Attachment size exceeds limit of 50 MB");
            }
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public MediaResponse toMediaResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .ownerId(media.getOwnerId())
                .contextType(media.getContextType())
                .contextId(media.getContextId())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .sizeBytes(media.getSizeBytes())
                .storageKey(media.getStorageKey())
                .cdnUrl(media.getCdnUrl())
                .hlsManifestUrl(media.getHlsManifestUrl())
                .durationSeconds(media.getDurationSeconds())
                .width(media.getWidth())
                .height(media.getHeight())
                .status(media.getStatus())
                .checksumSha256(media.getChecksumSha256())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }
}
