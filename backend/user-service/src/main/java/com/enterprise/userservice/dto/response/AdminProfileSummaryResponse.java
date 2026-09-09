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
public class AdminProfileSummaryResponse {

    private UUID userId;
    private String headline;
    private String avatarUrl;
    private String country;
    private Boolean isPublic;
    private Instant createdAt;

    public static AdminProfileSummaryResponse fromEntity(Profile profile) {
        if (profile == null) return null;
        return AdminProfileSummaryResponse.builder()
                .userId(profile.getUserId())
                .headline(profile.getHeadline())
                .avatarUrl(profile.getAvatarUrl())
                .country(profile.getCountry())
                .isPublic(profile.getIsPublic())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
