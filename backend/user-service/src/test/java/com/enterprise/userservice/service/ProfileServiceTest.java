package com.enterprise.userservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.userservice.dto.request.UpdateProfileRequest;
import com.enterprise.userservice.dto.response.AdminProfileSummaryResponse;
import com.enterprise.userservice.dto.response.ProfileResponse;
import com.enterprise.userservice.entity.Profile;
import com.enterprise.userservice.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        profile = Profile.builder()
                .userId(userId)
                .headline("Software Engineer")
                .bio("Building distributed systems")
                .language("en")
                .isPublic(true)
                .build();
    }

    @Test
    void getOrCreateProfile_WhenProfileExists_ReturnsExistingProfile() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getOrCreateProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("Software Engineer", response.getHeadline());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void getOrCreateProfile_WhenProfileDoesNotExist_AutoProvisionsProfile() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.getOrCreateProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.getIsPublic());
        assertEquals("en", response.getLanguage());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void updateProfile_UpdatesFieldsSuccessfully() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .headline("Staff Engineer")
                .bio("Distributed systems & AI")
                .country("India")
                .timezone("Asia/Kolkata")
                .website("https://example.com")
                .github("https://github.com/example")
                .isPublic(false)
                .build();

        ProfileResponse updated = profileService.updateProfile(userId, request);

        assertNotNull(updated);
        assertEquals("Staff Engineer", updated.getHeadline());
        assertEquals("Distributed systems & AI", updated.getBio());
        assertEquals("India", updated.getCountry());
        assertEquals("Asia/Kolkata", updated.getTimezone());
        assertEquals("https://example.com", updated.getWebsite());
        assertEquals("https://github.com/example", updated.getGithub());
        assertFalse(updated.getIsPublic());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void getPublicProfile_WhenPublic_ReturnsProfile() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getPublicProfile(userId, UUID.randomUUID(), List.of("STUDENT"));

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void getPublicProfile_WhenPrivateAndStranger_ThrowsAccessDenied() {
        profile.setIsPublic(false);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ApiException ex = assertThrows(ApiException.class, () ->
                profileService.getPublicProfile(userId, UUID.randomUUID(), List.of("STUDENT"))
        );

        assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void getPublicProfile_WhenPrivateAndOwner_ReturnsProfile() {
        profile.setIsPublic(false);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getPublicProfile(userId, userId, List.of("STUDENT"));

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void getPublicProfile_WhenPrivateAndAdmin_ReturnsProfile() {
        profile.setIsPublic(false);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getPublicProfile(userId, UUID.randomUUID(), List.of("ADMIN"));

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void getPublicProfile_WhenNotFound_ThrowsResourceNotFoundException() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                profileService.getPublicProfile(userId, null, null)
        );
    }

    @Test
    void searchProfiles_ReturnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Profile> page = new PageImpl<>(List.of(profile), pageable, 1);
        when(profileRepository.searchProfiles("Engineer", pageable)).thenReturn(page);

        Page<AdminProfileSummaryResponse> result = profileService.searchProfiles("Engineer", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Software Engineer", result.getContent().get(0).getHeadline());
    }
}
