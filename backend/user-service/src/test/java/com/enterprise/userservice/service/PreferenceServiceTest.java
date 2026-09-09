package com.enterprise.userservice.service;

import com.enterprise.userservice.dto.request.UpdatePreferenceRequest;
import com.enterprise.userservice.dto.response.UserPreferenceResponse;
import com.enterprise.userservice.entity.UserPreference;
import com.enterprise.userservice.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    private UUID userId;
    private UserPreference preference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        preference = UserPreference.builder()
                .userId(userId)
                .emailNotifications(true)
                .marketingEmails(false)
                .courseUpdates(true)
                .theme("system")
                .build();
    }

    @Test
    void getOrCreatePreferences_WhenExists_ReturnsExisting() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        UserPreferenceResponse response = preferenceService.getOrCreatePreferences(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.getEmailNotifications());
        assertFalse(response.getMarketingEmails());
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void getOrCreatePreferences_WhenDoesNotExist_AutoProvisionsDefault() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResponse response = preferenceService.getOrCreatePreferences(userId);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertTrue(response.getEmailNotifications());
        assertFalse(response.getMarketingEmails());
        assertTrue(response.getCourseUpdates());
        assertEquals("system", response.getTheme());
        verify(preferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void updatePreferences_UpdatesSpecifiedSettings() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePreferenceRequest request = UpdatePreferenceRequest.builder()
                .marketingEmails(true)
                .theme("dark")
                .build();

        UserPreferenceResponse response = preferenceService.updatePreferences(userId, request);

        assertNotNull(response);
        assertTrue(response.getMarketingEmails());
        assertEquals("dark", response.getTheme());
        assertTrue(response.getEmailNotifications()); // untouched
        verify(preferenceRepository).save(any(UserPreference.class));
    }
}
