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
public class UpdatePreferenceRequest {

    private Boolean emailNotifications;

    private Boolean marketingEmails;

    private Boolean courseUpdates;

    @Size(max = 20, message = "Theme must be at most 20 characters")
    private String theme;
}
