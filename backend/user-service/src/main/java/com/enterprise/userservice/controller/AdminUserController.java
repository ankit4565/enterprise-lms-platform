package com.enterprise.userservice.controller;

import com.enterprise.common.dto.ApiResponse;
import com.enterprise.userservice.dto.response.AdminProfileSummaryResponse;
import com.enterprise.userservice.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Administrative user search and profile discovery")
public class AdminUserController {

    private final ProfileService profileService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Search user profiles (Admin only)")
    public ResponseEntity<ApiResponse<Page<AdminProfileSummaryResponse>>> searchProfiles(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminProfileSummaryResponse> results = profileService.searchProfiles(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(results, "Profiles searched successfully"));
    }
}
