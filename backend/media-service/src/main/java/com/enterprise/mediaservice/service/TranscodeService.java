package com.enterprise.mediaservice.service;

import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.mediaservice.dto.request.RenditionInfo;
import com.enterprise.mediaservice.dto.request.TranscodeCallbackRequest;
import com.enterprise.mediaservice.entity.Media;
import com.enterprise.mediaservice.entity.MediaStatus;
import com.enterprise.mediaservice.entity.TranscodeJob;
import com.enterprise.mediaservice.entity.TranscodeStatus;
import com.enterprise.mediaservice.repository.MediaRepository;
import com.enterprise.mediaservice.repository.TranscodeJobRepository;
import com.enterprise.mediaservice.storage.StorageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeService {

    private final MediaRepository mediaRepository;
    private final TranscodeJobRepository transcodeJobRepository;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    private static final short MAX_ATTEMPTS = 3;

    @Transactional
    public TranscodeJob createAndQueueJob(Media media) {
        log.info("Queueing transcode job for media {}", media.getId());

        TranscodeJob job = TranscodeJob.builder()
                .media(media)
                .status(TranscodeStatus.PROCESSING)
                .attempt((short) 1)
                .startedAt(Instant.now())
                .build();

        return transcodeJobRepository.save(job);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> processTranscodeAsync(UUID mediaId, UUID jobId) {
        processTranscodeJob(mediaId, jobId);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void processTranscodeJob(UUID mediaId, UUID jobId) {
        log.info("Starting transcode simulation for media {} job {}", mediaId, jobId);

        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        TranscodeJob job = transcodeJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("TranscodeJob", "id", jobId));

        try {
            // Simulate video transcoding and HLS generation
            String cdnBase = storageProperties.getCdnBaseUrl();
            String masterManifestUrl = String.format("%s/hls/%s/master.m3u8", cdnBase, media.getId());

            List<RenditionInfo> renditions = generateDefaultRenditions(media.getId(), cdnBase);
            String renditionsJson = objectMapper.writeValueAsString(renditions);

            job.setStatus(TranscodeStatus.COMPLETED);
            job.setRenditions(renditionsJson);
            job.setFinishedAt(Instant.now());
            transcodeJobRepository.save(job);

            media.setStatus(MediaStatus.READY);
            media.setHlsManifestUrl(masterManifestUrl);
            media.setDurationSeconds(media.getDurationSeconds() != null && media.getDurationSeconds() > 0 ? media.getDurationSeconds() : 600); // 10 min default
            media.setWidth(1920);
            media.setHeight(1080);
            mediaRepository.save(media);

            log.info("Transcoding successfully completed for media {}. Manifest: {}", mediaId, masterManifestUrl);

        } catch (Exception e) {
            log.error("Transcode failed for media {} on attempt {}: {}", mediaId, job.getAttempt(), e.getMessage());
            handleTranscodeFailure(media, job, e.getMessage());
        }
    }

    @Transactional
    public void handleTranscodeFailure(Media media, TranscodeJob job, String errorMessage) {
        if (job.getAttempt() < MAX_ATTEMPTS) {
            short nextAttempt = (short) (job.getAttempt() + 1);
            job.setAttempt(nextAttempt);
            job.setStatus(TranscodeStatus.PROCESSING);
            job.setError(String.format("Attempt %d failed: %s", job.getAttempt(), errorMessage));
            transcodeJobRepository.save(job);
            log.warn("Retrying transcode job {} for media {}, attempt {}", job.getId(), media.getId(), nextAttempt);
            // In a production worker, this would be requeued to RabbitMQ with exponential backoff delay
        } else {
            job.setStatus(TranscodeStatus.FAILED);
            job.setError(String.format("Failed after %d attempts: %s", MAX_ATTEMPTS, errorMessage));
            job.setFinishedAt(Instant.now());
            transcodeJobRepository.save(job);

            media.setStatus(MediaStatus.FAILED);
            mediaRepository.save(media);
            log.error("Media {} permanently FAILED transcoding after {} attempts", media.getId(), MAX_ATTEMPTS);
        }
    }

    @Transactional
    public void applyTranscodeCallback(UUID mediaId, TranscodeCallbackRequest request) {
        Media media = mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));

        TranscodeJob job = transcodeJobRepository.findTopByMediaIdOrderByCreatedAtDesc(mediaId)
                .orElseGet(() -> TranscodeJob.builder().media(media).build());

        if (request.getStatus() == TranscodeStatus.COMPLETED) {
            job.setStatus(TranscodeStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            try {
                if (request.getRenditions() != null) {
                    job.setRenditions(objectMapper.writeValueAsString(request.getRenditions()));
                }
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize renditions JSON: {}", e.getMessage());
            }
            transcodeJobRepository.save(job);

            media.setStatus(MediaStatus.READY);
            if (request.getHlsManifestUrl() != null) {
                media.setHlsManifestUrl(request.getHlsManifestUrl());
            }
            if (request.getDurationSeconds() != null) {
                media.setDurationSeconds(request.getDurationSeconds());
            }
            if (request.getWidth() != null) {
                media.setWidth(request.getWidth());
            }
            if (request.getHeight() != null) {
                media.setHeight(request.getHeight());
            }
            mediaRepository.save(media);
            log.info("Applied transcode callback SUCCESS for media {}", mediaId);

        } else if (request.getStatus() == TranscodeStatus.FAILED) {
            job.setStatus(TranscodeStatus.FAILED);
            job.setError(request.getError());
            job.setFinishedAt(Instant.now());
            transcodeJobRepository.save(job);

            media.setStatus(MediaStatus.FAILED);
            mediaRepository.save(media);
            log.warn("Applied transcode callback FAILED for media {}: {}", mediaId, request.getError());
        }
    }

    private List<RenditionInfo> generateDefaultRenditions(UUID mediaId, String cdnBase) {
        List<RenditionInfo> renditions = new ArrayList<>();
        renditions.add(new RenditionInfo("1080p", String.format("%s/hls/%s/1080p.m3u8", cdnBase, mediaId), 5000000, 1920, 1080));
        renditions.add(new RenditionInfo("720p", String.format("%s/hls/%s/720p.m3u8", cdnBase, mediaId), 2800000, 1280, 720));
        renditions.add(new RenditionInfo("480p", String.format("%s/hls/%s/480p.m3u8", cdnBase, mediaId), 1400000, 854, 480));
        renditions.add(new RenditionInfo("360p", String.format("%s/hls/%s/360p.m3u8", cdnBase, mediaId), 800000, 640, 360));
        return renditions;
    }
}
