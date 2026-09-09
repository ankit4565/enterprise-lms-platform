package com.enterprise.userservice.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(length = 160)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String country;

    @Column(length = 50)
    private String timezone;

    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Column(length = 255)
    private String website;

    @Column(length = 255)
    private String linkedin;

    @Column(length = 255)
    private String github;

    @Column(name = "expertise_tags", columnDefinition = "TEXT")
    private String expertiseTags;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;
}
