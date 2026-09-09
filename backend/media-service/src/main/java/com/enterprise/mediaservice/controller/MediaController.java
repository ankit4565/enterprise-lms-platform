package com.enterprise.mediaservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.mediaservice.dto.request.CompleteUploadRequest;
import com.enterprise.mediaservice.dto.request.CreateUploadUrlRequest;
import com.enterprise.mediaservice.dto.request.TranscodeCallbackRequest;
import com.enterprise.mediaservice.dto.response.MediaResponse;
import com.enterprise.mediaservice.dto.response.StreamPlaybackResponse;
import com.enterprise.mediaservice.dto.response.UploadUrlResponse;
import com.enterprise.mediaservice.security.UserPrincipal;
import com.enterprise.mediaservice.service.MediaService;
import com.enterprise.mediaservice.service.TranscodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "Endpoints for presigned multipart uploads, media metadata, and secure streaming")
public class MediaController {

    private final MediaService mediaService;
    private final TranscodeService transcodeService;

    @PostMapping("/upload-url")
    @Operation(summary = "Generate presigned multipart S3 upload URLs")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateUploadUrlRequest request
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to generate upload URLs");
        }

        // For video uploads, require instructor or admin role
        if (request.getContextType().isVideo() && !principal.isInstructor()) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only instructors and administrators can upload course videos");
        }

        UploadUrlResponse response = mediaService.createUploadUrl(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Upload session created"));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Finalize multipart upload and trigger processing")
    public ResponseEntity<ApiResponse<MediaResponse>> completeUpload(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteUploadRequest request
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to finalize uploads");
        }

        MediaResponse response = mediaService.completeUpload(principal.getUserId(), id, request, principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok(response, "Upload completed successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media metadata and status")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to access media metadata");
        }

        MediaResponse response = mediaService.getMedia(principal.getUserId(), id, principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/stream")
    @Operation(summary = "Get signed playback stream or manifest URL")
    public ResponseEntity<ApiResponse<StreamPlaybackResponse>> getStreamPlayback(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to stream playback media");
        }

        StreamPlaybackResponse response = mediaService.getStreamPlayback(principal.getUserId(), id, principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete media resource")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to delete media");
        }

        mediaService.deleteMedia(principal.getUserId(), id, principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok(null, "Media deleted successfully"));
    }

    @GetMapping("/my")
    @Operation(summary = "List all media uploaded by the current user")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMyMedia(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required to list media");
        }

        List<MediaResponse> response = mediaService.listMediaByOwner(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/transcode-callback")
    @Operation(summary = "Transcoder worker callback for status and renditions update")
    public ResponseEntity<ApiResponse<Void>> applyTranscodeCallback(
            @PathVariable UUID id,
            @Valid @RequestBody TranscodeCallbackRequest request
    ) {
        transcodeService.applyTranscodeCallback(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Transcode callback applied successfully"));
    }
}
