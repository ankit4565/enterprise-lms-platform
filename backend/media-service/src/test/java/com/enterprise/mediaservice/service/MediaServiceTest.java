package com.enterprise.mediaservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.BadRequestException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.mediaservice.dto.request.CompleteUploadRequest;
import com.enterprise.mediaservice.dto.request.CreateUploadUrlRequest;
import com.enterprise.mediaservice.dto.request.PartInfo;
import com.enterprise.mediaservice.dto.response.MediaResponse;
import com.enterprise.mediaservice.dto.response.StreamPlaybackResponse;
import com.enterprise.mediaservice.dto.response.UploadUrlResponse;
import com.enterprise.mediaservice.entity.*;
import com.enterprise.mediaservice.repository.MediaRepository;
import com.enterprise.mediaservice.repository.UploadSessionRepository;
import com.enterprise.mediaservice.storage.MediaProperties;
import com.enterprise.mediaservice.storage.StorageProperties;
import com.enterprise.mediaservice.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private TranscodeService transcodeService;

    @Spy
    private MediaProperties mediaProperties = new MediaProperties();

    @Spy
    private StorageProperties storageProperties = new StorageProperties();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MediaService mediaService;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
    }

    @Test
    void createUploadUrl_Success_Video() {
        CreateUploadUrlRequest request = CreateUploadUrlRequest.builder()
                .filename("intro.mp4")
                .mimeType("video/mp4")
                .sizeBytes(25_000_000L) // 25 MB -> 3 parts with 10 MB default part size
                .contextType(MediaContextType.COURSE_VIDEO)
                .contextId(UUID.randomUUID())
                .build();

        when(storageService.initiateMultipartUpload(anyString(), eq("video/mp4"))).thenReturn("upload-123");
        when(storageService.generatePresignedPartUploadUrl(anyString(), eq("upload-123"), anyInt(), any(Duration.class)))
                .thenReturn("http://s3/part-url");
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UploadUrlResponse response = mediaService.createUploadUrl(ownerId, request);

        assertNotNull(response);
        assertEquals("upload-123", response.getUploadId());
        assertEquals(3, response.getTotalParts());
        assertEquals(3, response.getPartUrls().size());
        verify(mediaRepository).save(any(Media.class));
        verify(uploadSessionRepository).save(any(UploadSession.class));
    }

    @Test
    void createUploadUrl_Fails_WhenInvalidMime() {
        CreateUploadUrlRequest request = CreateUploadUrlRequest.builder()
                .filename("script.exe")
                .mimeType("application/x-msdownload")
                .sizeBytes(1024L)
                .contextType(MediaContextType.COURSE_VIDEO)
                .build();

        assertThrows(BadRequestException.class, () ->
                mediaService.createUploadUrl(ownerId, request)
        );
    }

    @Test
    void createUploadUrl_Fails_WhenFileTooLarge() {
        CreateUploadUrlRequest request = CreateUploadUrlRequest.builder()
                .filename("massive.mp4")
                .mimeType("video/mp4")
                .sizeBytes(3_000_000_000L) // 3 GB > 2 GB limit
                .contextType(MediaContextType.COURSE_VIDEO)
                .build();

        assertThrows(BadRequestException.class, () ->
                mediaService.createUploadUrl(ownerId, request)
        );
    }

    @Test
    void completeUpload_Success_Video() {
        UUID mediaId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .contextType(MediaContextType.COURSE_VIDEO)
                .originalFilename("lesson.mp4")
                .mimeType("video/mp4")
                .sizeBytes(10_000_000L)
                .storageKey("uploads/" + ownerId + "/" + mediaId + "/lesson.mp4")
                .status(MediaStatus.PENDING)
                .build();

        UploadSession session = UploadSession.builder()
                .media(media)
                .uploadId("upload-123")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        CompleteUploadRequest request = CompleteUploadRequest.builder()
                .uploadId("upload-123")
                .parts(List.of(new PartInfo(1, "\"etag-1\"")))
                .checksumSha256("abc123sha")
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(uploadSessionRepository.findByMediaId(mediaId)).thenReturn(Optional.of(session));
        when(transcodeService.createAndQueueJob(any(Media.class))).thenReturn(
                TranscodeJob.builder().id(UUID.randomUUID()).media(media).status(TranscodeStatus.PROCESSING).build()
        );

        MediaResponse response = mediaService.completeUpload(ownerId, mediaId, request, false);

        assertNotNull(response);
        assertEquals(MediaStatus.PROCESSING, response.getStatus());
        assertEquals("abc123sha", response.getChecksumSha256());
        assertNotNull(session.getCompletedAt());
        verify(storageService).completeMultipartUpload(eq(media.getStorageKey()), eq("upload-123"), anyList());
        verify(transcodeService).createAndQueueJob(any(Media.class));
    }

    @Test
    void completeUpload_Success_Image() {
        UUID mediaId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .contextType(MediaContextType.THUMBNAIL)
                .originalFilename("cover.png")
                .mimeType("image/png")
                .sizeBytes(500_000L)
                .storageKey("uploads/" + ownerId + "/" + mediaId + "/cover.png")
                .status(MediaStatus.PENDING)
                .build();

        UploadSession session = UploadSession.builder()
                .media(media)
                .uploadId("upload-img-1")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        CompleteUploadRequest request = CompleteUploadRequest.builder()
                .uploadId("upload-img-1")
                .parts(List.of(new PartInfo(1, "\"etag-img\"")))
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));
        when(uploadSessionRepository.findByMediaId(mediaId)).thenReturn(Optional.of(session));

        MediaResponse response = mediaService.completeUpload(ownerId, mediaId, request, false);

        assertNotNull(response);
        assertEquals(MediaStatus.READY, response.getStatus());
        assertNotNull(response.getCdnUrl());
        verify(transcodeService, never()).createAndQueueJob(any());
    }

    @Test
    void completeUpload_Throws_WhenNotOwnerOrAdmin() {
        UUID mediaId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .status(MediaStatus.PENDING)
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));

        CompleteUploadRequest request = CompleteUploadRequest.builder().uploadId("up").parts(List.of()).build();

        ApiException ex = assertThrows(ApiException.class, () ->
                mediaService.completeUpload(differentUserId, mediaId, request, false)
        );
        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getStreamPlayback_Success_WhenReady() {
        UUID mediaId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .hlsManifestUrl("http://cdn/hls/" + mediaId + "/master.m3u8")
                .mimeType("application/x-mpegURL")
                .durationSeconds(420)
                .status(MediaStatus.READY)
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));

        StreamPlaybackResponse response = mediaService.getStreamPlayback(ownerId, mediaId, false);

        assertNotNull(response);
        assertEquals("http://cdn/hls/" + mediaId + "/master.m3u8", response.getStreamUrl());
        assertEquals(420, response.getDurationSeconds());
    }

    @Test
    void getStreamPlayback_Throws_WhenNotReady() {
        UUID mediaId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .status(MediaStatus.PROCESSING)
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));

        assertThrows(BadRequestException.class, () ->
                mediaService.getStreamPlayback(ownerId, mediaId, false)
        );
    }

    @Test
    void deleteMedia_Success() {
        UUID mediaId = UUID.randomUUID();
        Media media = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .storageKey("key/path")
                .isDeleted(false)
                .build();

        when(mediaRepository.findByIdAndIsDeletedFalse(mediaId)).thenReturn(Optional.of(media));

        mediaService.deleteMedia(ownerId, mediaId, false);

        assertTrue(media.isDeleted());
        assertNotNull(media.getDeletedAt());
        verify(storageService).deleteObject("key/path");
        verify(mediaRepository).save(media);
    }
}
