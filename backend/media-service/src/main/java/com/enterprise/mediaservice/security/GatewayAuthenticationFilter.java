package com.enterprise.mediaservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_PERMS = "X-Perms";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);

        if (StringUtils.hasText(userIdHeader)) {
            try {
                UUID userId = UUID.fromString(userIdHeader.trim());
                String email = request.getHeader(HEADER_USER_EMAIL);

                List<String> rawRoles = parseCommaSeparatedHeader(request.getHeader(HEADER_ROLES));
                List<String> rawPerms = parseCommaSeparatedHeader(request.getHeader(HEADER_PERMS));

                Set<GrantedAuthority> authorities = new HashSet<>();

                for (String role : rawRoles) {
                    String authorityName = role.toUpperCase().startsWith("ROLE_")
                            ? role.toUpperCase()
                            : "ROLE_" + role.toUpperCase();
                    authorities.add(new SimpleGrantedAuthority(authorityName));
                }

                for (String perm : rawPerms) {
                    authorities.add(new SimpleGrantedAuthority(perm.toUpperCase()));
                }

                UserPrincipal principal = UserPrincipal.builder()
                        .userId(userId)
                        .email(email != null ? email.trim() : null)
                        .roles(rawRoles)
                        .permissions(rawPerms)
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format for {}: {}", HEADER_USER_ID, userIdHeader);
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<String> parseCommaSeparatedHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return Collections.emptyList();
        }
        return Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
