package com.enterprise.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<String> getCurrentUserEmail() {
        return getAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }

    public static Optional<UUID> getCurrentUserId() {
        return getAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(auth -> {
                    Object details = auth.getDetails();
                    if (details instanceof UUID uuid) {
                        return uuid;
                    }
                    try {
                        return UUID.fromString(auth.getName());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
    }

    public static List<String> getCurrentUserRoles() {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean hasRole(String role) {
        String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentUserRoles().contains(prefixedRole);
    }
}
