package com.enterprise.userservice.service;

import com.enterprise.userservice.dto.request.UpdatePreferenceRequest;
import com.enterprise.userservice.dto.response.UserPreferenceResponse;
import com.enterprise.userservice.entity.UserPreference;
import com.enterprise.userservice.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    @Transactional
    public UserPreferenceResponse getOrCreatePreferences(UUID userId) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Auto-provisioning default preferences for user {}", userId);
                    UserPreference newPref = UserPreference.builder()
                            .userId(userId)
                            .emailNotifications(true)
                            .marketingEmails(false)
                            .courseUpdates(true)
                            .theme("system")
                            .build();
                    return preferenceRepository.save(newPref);
                });
        return UserPreferenceResponse.fromEntity(preference);
    }

    @Transactional
    public UserPreferenceResponse updatePreferences(UUID userId, UpdatePreferenceRequest request) {
        UserPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder()
                        .userId(userId)
                        .emailNotifications(true)
                        .marketingEmails(false)
                        .courseUpdates(true)
                        .theme("system")
                        .build());

        if (request.getEmailNotifications() != null) {
            preference.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getMarketingEmails() != null) {
            preference.setMarketingEmails(request.getMarketingEmails());
        }
        if (request.getCourseUpdates() != null) {
            preference.setCourseUpdates(request.getCourseUpdates());
        }
        if (request.getTheme() != null) {
            preference.setTheme(request.getTheme());
        }

        UserPreference saved = preferenceRepository.save(preference);
        log.info("Preferences updated for user {}", userId);
        return UserPreferenceResponse.fromEntity(saved);
    }
}
