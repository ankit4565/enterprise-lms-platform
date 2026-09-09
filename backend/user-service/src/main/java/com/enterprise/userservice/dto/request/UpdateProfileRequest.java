package com.enterprise.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 160, message = "Headline must be at most 160 characters")
    private String headline;

    private String bio;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone;

    @Size(max = 10, message = "Language must be at most 10 characters")
    private String language;

    @Size(max = 255, message = "Website must be at most 255 characters")
    private String website;

    @Size(max = 255, message = "LinkedIn must be at most 255 characters")
    private String linkedin;

    @Size(max = 255, message = "GitHub must be at most 255 characters")
    private String github;

    private String expertiseTags;

    private Boolean isPublic;
}
