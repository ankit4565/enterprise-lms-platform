package com.enterprise.mediaservice.service;

import com.enterprise.mediaservice.dto.request.RenditionInfo;
import com.enterprise.mediaservice.dto.request.TranscodeCallbackRequest;
import com.enterprise.mediaservice.entity.Media;
import com.enterprise.mediaservice.entity.MediaStatus;
import com.enterprise.mediaservice.entity.TranscodeJob;
import com.enterprise.mediaservice.entity.TranscodeStatus;
import com.enterprise.mediaservice.repository.MediaRepository;
import com.enterprise.mediaservice.repository.TranscodeJobRepository;
import com.enterprise.mediaservice.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranscodeServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private TranscodeJobRepository transcodeJobRepository;

    @Spy
    private StorageProperties storageProperties = new StorageProperties();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TranscodeService transcodeService;

    private UUID mediaId;
    private UUID jobId;
    private Media media;
    private TranscodeJob job;

    @BeforeEach
    void setUp() {
        mediaId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        media = Media.builder()
                .id(mediaId)
                .ownerId(UUID.randomUUID())
                .originalFilename("video.mp4")
                .status(MediaStatus.PROCESSING)
                .build();

        job = TranscodeJob.builder()
                .id(jobId)
                .media(media)
                .status(TranscodeStatus.PROCESSING)
                .attempt((short) 1)
                .build();
    }

    @Test
    void processTranscodeJob_Success() {
        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(transcodeJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        transcodeService.processTranscodeJob(mediaId, jobId);

        assertEquals(TranscodeStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getRenditions());
        assertNotNull(job.getFinishedAt());

        assertEquals(MediaStatus.READY, media.getStatus());
        assertNotNull(media.getHlsManifestUrl());
        assertTrue(media.getHlsManifestUrl().endsWith("master.m3u8"));
        assertEquals(1920, media.getWidth());
        assertEquals(1080, media.getHeight());

        verify(transcodeJobRepository).save(job);
        verify(mediaRepository).save(media);
    }

    @Test
    void handleTranscodeFailure_Retries_WhenUnderMaxAttempts() {
        job.setAttempt((short) 1);

        transcodeService.handleTranscodeFailure(media, job, "FFmpeg error 137");

        assertEquals((short) 2, job.getAttempt());
        assertEquals(TranscodeStatus.PROCESSING, job.getStatus());
        assertTrue(job.getError().contains("FFmpeg error 137"));
        verify(transcodeJobRepository).save(job);
        verify(mediaRepository, never()).save(media);
    }

    @Test
    void handleTranscodeFailure_MarksFailed_WhenMaxAttemptsReached() {
        job.setAttempt((short) 3);

        transcodeService.handleTranscodeFailure(media, job, "Corrupt video file");

        assertEquals(TranscodeStatus.FAILED, job.getStatus());
        assertEquals(MediaStatus.FAILED, media.getStatus());
        assertNotNull(job.getFinishedAt());
        verify(transcodeJobRepository).save(job);
        verify(mediaRepository).save(media);
    }

    @Test
    void applyTranscodeCallback_Success() {
        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(transcodeJobRepository.findTopByMediaIdOrderByCreatedAtDesc(mediaId)).thenReturn(Optional.of(job));

        TranscodeCallbackRequest request = TranscodeCallbackRequest.builder()
                .status(TranscodeStatus.COMPLETED)
                .hlsManifestUrl("http://cdn/hls/custom/master.m3u8")
                .durationSeconds(750)
                .width(3840)
                .height(2160)
                .renditions(List.of(new RenditionInfo("4K", "http://cdn/4k.m3u8", 12000000, 3840, 2160)))
                .build();

        transcodeService.applyTranscodeCallback(mediaId, request);

        assertEquals(MediaStatus.READY, media.getStatus());
        assertEquals("http://cdn/hls/custom/master.m3u8", media.getHlsManifestUrl());
        assertEquals(750, media.getDurationSeconds());
        assertEquals(3840, media.getWidth());
        assertEquals(2160, media.getHeight());
        assertEquals(TranscodeStatus.COMPLETED, job.getStatus());

        verify(mediaRepository).save(media);
        verify(transcodeJobRepository).save(job);
    }
}
