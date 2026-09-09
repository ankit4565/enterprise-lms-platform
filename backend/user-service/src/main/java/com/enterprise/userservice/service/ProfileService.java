package com.enterprise.userservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.userservice.dto.request.UpdateProfileRequest;
import com.enterprise.userservice.dto.response.AdminProfileSummaryResponse;
import com.enterprise.userservice.dto.response.ProfileResponse;
import com.enterprise.userservice.entity.Profile;
import com.enterprise.userservice.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional
    public ProfileResponse getOrCreateProfile(UUID userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Auto-provisioning initial profile for user {}", userId);
                    Profile newProfile = Profile.builder()
                            .userId(userId)
                            .language("en")
                            .isPublic(true)
                            .build();
                    return profileRepository.save(newProfile);
                });
        return ProfileResponse.fromEntity(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> Profile.builder()
                        .userId(userId)
                        .language("en")
                        .isPublic(true)
                        .build());

        if (request.getHeadline() != null) {
            profile.setHeadline(request.getHeadline());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }
        if (request.getLanguage() != null) {
            profile.setLanguage(request.getLanguage());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
        if (request.getLinkedin() != null) {
            profile.setLinkedin(request.getLinkedin());
        }
        if (request.getGithub() != null) {
            profile.setGithub(request.getGithub());
        }
        if (request.getExpertiseTags() != null) {
            profile.setExpertiseTags(request.getExpertiseTags());
        }
        if (request.getIsPublic() != null) {
            profile.setIsPublic(request.getIsPublic());
        }

        Profile saved = profileRepository.save(profile);
        log.info("Profile updated for user {}", userId);
        return ProfileResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(UUID targetUserId, UUID requesterId, List<String> requesterRoles) {
        Profile profile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + targetUserId));

        if (!Boolean.TRUE.equals(profile.getIsPublic())) {
            boolean isOwner = requesterId != null && requesterId.equals(targetUserId);
            boolean isAdmin = requesterRoles != null && (requesterRoles.contains("ADMIN") || requesterRoles.contains("SUPER_ADMIN"));

            if (!isOwner && !isAdmin) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "This profile is private");
            }
        }

        return ProfileResponse.fromEntity(profile);
    }

    @Transactional(readOnly = true)
    public Page<AdminProfileSummaryResponse> searchProfiles(String keyword, Pageable pageable) {
        return profileRepository.searchProfiles(keyword, pageable)
                .map(AdminProfileSummaryResponse::fromEntity);
    }
}
