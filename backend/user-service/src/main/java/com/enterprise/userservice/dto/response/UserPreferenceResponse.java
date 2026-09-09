package com.enterprise.userservice.dto.response;

import com.enterprise.userservice.entity.UserPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {

    private UUID userId;
    private Boolean emailNotifications;
    private Boolean marketingEmails;
    private Boolean courseUpdates;
    private String theme;
    private Instant updatedAt;

    public static UserPreferenceResponse fromEntity(UserPreference preference) {
        if (preference == null) return null;
        return UserPreferenceResponse.builder()
                .userId(preference.getUserId())
                .emailNotifications(preference.getEmailNotifications())
                .marketingEmails(preference.getMarketingEmails())
                .courseUpdates(preference.getCourseUpdates())
                .theme(preference.getTheme())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
