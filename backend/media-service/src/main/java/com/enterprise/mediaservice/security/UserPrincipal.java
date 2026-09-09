package com.enterprise.mediaservice.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {

    private UUID userId;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Collection<? extends GrantedAuthority> authorities;

    public boolean hasRole(String role) {
        if (roles == null) return false;
        String normalized = role.toUpperCase().replace("ROLE_", "");
        return roles.stream()
                .map(r -> r.toUpperCase().replace("ROLE_", ""))
                .anyMatch(r -> r.equals(normalized));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("SUPER_ADMIN");
    }

    public boolean isInstructor() {
        return hasRole("INSTRUCTOR") || isAdmin();
    }
}
