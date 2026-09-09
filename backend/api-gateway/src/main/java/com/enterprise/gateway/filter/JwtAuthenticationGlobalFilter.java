package com.enterprise.gateway.filter;

import com.enterprise.gateway.security.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenValidator jwtValidator;

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/otp",
            "/api/v1/auth/refresh",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        boolean isPublicPath = isPublic(path);
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (isPublicPath) {
            // If token is optionally provided on a public endpoint, enrich headers
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtValidator.validateToken(token)) {
                    ServerHttpRequest mutatedRequest = enrichRequestHeaders(request, token);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }
            }
            return chain.filter(exchange);
        }

        // Secured endpoint: token is required
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for secured path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Authorization header with Bearer token is required");
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.validateToken(token)) {
            log.warn("Invalid JWT token for secured path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        ServerHttpRequest mutatedRequest = enrichRequestHeaders(request, token);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private ServerHttpRequest enrichRequestHeaders(ServerHttpRequest request, String token) {
        String userId = jwtValidator.extractUserId(token);
        String email = jwtValidator.extractEmail(token);
        List<String> roles = jwtValidator.extractRoles(token);
        List<String> perms = jwtValidator.extractPermissions(token);

        ServerHttpRequest.Builder builder = request.mutate();
        if (userId != null) {
            builder.header("X-User-Id", userId);
        }
        if (email != null) {
            builder.header("X-User-Email", email);
        }
        if (!roles.isEmpty()) {
            builder.header("X-Roles", String.join(",", roles));
        }
        if (!perms.isEmpty()) {
            builder.header("X-Perms", String.join(",", perms));
        }

        return builder.build();
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        String jsonResponse = String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"errors\":null,\"timestamp\":\"%s\",\"path\":\"%s\",\"correlationId\":%s}",
                escapeJson(message),
                Instant.now().toString(),
                escapeJson(path),
                correlationId != null ? "\"" + escapeJson(correlationId) + "\"" : "null"
        );

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"");
    }

    @Override
    public int getOrder() {
        return -1; // Execute after CorrelationIdGlobalFilter (-2)
    }
}
