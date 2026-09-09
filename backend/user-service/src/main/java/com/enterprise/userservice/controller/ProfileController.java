package com.enterprise.userservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.userservice.dto.request.UpdateProfileRequest;
import com.enterprise.userservice.dto.response.ProfileResponse;
import com.enterprise.userservice.security.UserPrincipal;
import com.enterprise.userservice.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "User profile retrieval and management")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse profile = profileService.getOrCreateProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        ProfileResponse updated = profileService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get public profile by user ID")
    public ResponseEntity<ApiResponse<ProfileResponse>> getPublicProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID requesterId = principal != null ? principal.getUserId() : null;
        List<String> requesterRoles = principal != null ? principal.getRoles() : null;

        ProfileResponse profile = profileService.getPublicProfile(userId, requesterId, requesterRoles);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile retrieved successfully"));
    }
}
