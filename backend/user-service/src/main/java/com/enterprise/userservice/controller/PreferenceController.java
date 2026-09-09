package com.enterprise.userservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.userservice.dto.request.UpdatePreferenceRequest;
import com.enterprise.userservice.dto.response.UserPreferenceResponse;
import com.enterprise.userservice.security.UserPrincipal;
import com.enterprise.userservice.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Notification and display preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get current user preferences")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getMyPreferences(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserPreferenceResponse preferences = preferenceService.getOrCreatePreferences(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(preferences, "Preferences retrieved successfully"));
    }

    @PutMapping
    @Operation(summary = "Update current user preferences")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateMyPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferenceRequest request
    ) {
        UserPreferenceResponse updated = preferenceService.updatePreferences(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Preferences updated successfully"));
    }
}
