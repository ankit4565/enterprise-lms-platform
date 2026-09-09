package com.enterprise.userservice.dto.response;

import com.enterprise.userservice.entity.Profile;
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
public class ProfileResponse {

    private UUID userId;
    private String headline;
    private String bio;
    private String avatarUrl;
    private String phone;
    private String country;
    private String timezone;
    private String language;
    private String website;
    private String linkedin;
    private String github;
    private String expertiseTags;
    private Boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProfileResponse fromEntity(Profile profile) {
        if (profile == null) return null;
        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .phone(profile.getPhone())
                .country(profile.getCountry())
                .timezone(profile.getTimezone())
                .language(profile.getLanguage())
                .website(profile.getWebsite())
                .linkedin(profile.getLinkedin())
                .github(profile.getGithub())
                .expertiseTags(profile.getExpertiseTags())
                .isPublic(profile.getIsPublic())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
