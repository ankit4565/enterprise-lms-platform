package com.enterprise.mediaservice.controller;

import com.enterprise.common.exception.GlobalExceptionHandler;
import com.enterprise.mediaservice.dto.request.CompleteUploadRequest;
import com.enterprise.mediaservice.dto.request.CreateUploadUrlRequest;
import com.enterprise.mediaservice.dto.request.PartInfo;
import com.enterprise.mediaservice.dto.request.TranscodeCallbackRequest;
import com.enterprise.mediaservice.dto.response.MediaResponse;
import com.enterprise.mediaservice.dto.response.PartUploadUrl;
import com.enterprise.mediaservice.dto.response.StreamPlaybackResponse;
import com.enterprise.mediaservice.dto.response.UploadUrlResponse;
import com.enterprise.mediaservice.entity.MediaContextType;
import com.enterprise.mediaservice.entity.MediaStatus;
import com.enterprise.mediaservice.entity.TranscodeStatus;
import com.enterprise.mediaservice.security.UserPrincipal;
import com.enterprise.mediaservice.service.MediaService;
import com.enterprise.mediaservice.service.TranscodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MediaService mediaService;

    @Mock
    private TranscodeService transcodeService;

    @InjectMocks
    private MediaController mediaController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testUserId;
    private UserPrincipal testInstructorPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testInstructorPrincipal = UserPrincipal.builder()
                .userId(testUserId)
                .email("instructor@example.com")
                .roles(List.of("INSTRUCTOR"))
                .permissions(Collections.emptyList())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(mediaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory
                    ) {
                        return testInstructorPrincipal;
                    }
                })
                .build();
    }

    @Test
    void createUploadUrl_Success() throws Exception {
        CreateUploadUrlRequest request = CreateUploadUrlRequest.builder()
                .filename("lesson-1.mp4")
                .mimeType("video/mp4")
                .sizeBytes(15_000_000L)
                .contextType(MediaContextType.COURSE_VIDEO)
                .contextId(UUID.randomUUID())
                .build();

        UploadUrlResponse response = UploadUrlResponse.builder()
                .mediaId(UUID.randomUUID())
                .uploadId("s3-up-id")
                .storageKey("uploads/path")
                .totalSizeBytes(15_000_000L)
                .totalParts(2)
                .partUrls(List.of(
                        new PartUploadUrl(1, "http://s3/part1", 10_000_000L),
                        new PartUploadUrl(2, "http://s3/part2", 5_000_000L)
                ))
                .expiresInSeconds(86400L)
                .build();

        when(mediaService.createUploadUrl(eq(testUserId), any(CreateUploadUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/media/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadId").value("s3-up-id"))
                .andExpect(jsonPath("$.data.totalParts").value(2))
                .andExpect(jsonPath("$.data.partUrls.length()").value(2));
    }

    @Test
    void completeUpload_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();
        CompleteUploadRequest request = CompleteUploadRequest.builder()
                .uploadId("s3-up-id")
                .parts(List.of(new PartInfo(1, "\"etag1\"")))
                .checksumSha256("sha-hash")
                .build();

        MediaResponse response = MediaResponse.builder()
                .id(mediaId)
                .ownerId(testUserId)
                .contextType(MediaContextType.COURSE_VIDEO)
                .originalFilename("lesson-1.mp4")
                .mimeType("video/mp4")
                .sizeBytes(15_000_000L)
                .status(MediaStatus.PROCESSING)
                .build();

        when(mediaService.completeUpload(eq(testUserId), eq(mediaId), any(CompleteUploadRequest.class), eq(false)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/media/{id}/complete", mediaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void getMedia_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();
        MediaResponse response = MediaResponse.builder()
                .id(mediaId)
                .ownerId(testUserId)
                .contextType(MediaContextType.COURSE_VIDEO)
                .originalFilename("lesson-1.mp4")
                .status(MediaStatus.READY)
                .hlsManifestUrl("http://cdn/hls/master.m3u8")
                .build();

        when(mediaService.getMedia(eq(testUserId), eq(mediaId), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(mediaId.toString()))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.hlsManifestUrl").value("http://cdn/hls/master.m3u8"));
    }

    @Test
    void getStreamPlayback_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();
        StreamPlaybackResponse response = StreamPlaybackResponse.builder()
                .mediaId(mediaId)
                .streamUrl("http://cdn/hls/master.m3u8")
                .mimeType("application/x-mpegURL")
                .durationSeconds(600)
                .expiresInSeconds(1800L)
                .build();

        when(mediaService.getStreamPlayback(eq(testUserId), eq(mediaId), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/media/{id}/stream", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.streamUrl").value("http://cdn/hls/master.m3u8"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(1800));
    }

    @Test
    void deleteMedia_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media deleted successfully"));
    }

    @Test
    void applyTranscodeCallback_Success() throws Exception {
        UUID mediaId = UUID.randomUUID();
        TranscodeCallbackRequest request = TranscodeCallbackRequest.builder()
                .status(TranscodeStatus.COMPLETED)
                .hlsManifestUrl("http://cdn/hls/master.m3u8")
                .durationSeconds(360)
                .width(1920)
                .height(1080)
                .build();

        mockMvc.perform(post("/api/v1/media/{id}/transcode-callback", mediaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transcode callback applied successfully"));
    }
}
