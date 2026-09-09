package com.enterprise.gateway.filter;

import com.enterprise.gateway.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGlobalFilterTest {

    @Mock
    private JwtTokenValidator jwtValidator;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthenticationGlobalFilter filter;
    private AtomicReference<ServerHttpRequest> capturedRequest;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationGlobalFilter(jwtValidator);
        capturedRequest = new AtomicReference<>();

        lenient().when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            capturedRequest.set(exchange.getRequest());
            return Mono.empty();
        });
    }

    @Test
    void filter_PublicPath_NoToken_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any());
        verify(jwtValidator, never()).validateToken(anyString());
    }

    @Test
    void filter_SecuredPath_MissingToken_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_SecuredPath_InvalidToken_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("Authorization", "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtValidator.validateToken("invalid-token")).thenReturn(false);

        filter.filter(exchange, filterChain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_SecuredPath_ValidToken_InjectsHeadersAndPassesThrough() {
        String userId = UUID.randomUUID().toString();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("Authorization", "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtValidator.validateToken("valid-token")).thenReturn(true);
        when(jwtValidator.extractUserId("valid-token")).thenReturn(userId);
        when(jwtValidator.extractEmail("valid-token")).thenReturn("learner@example.com");
        when(jwtValidator.extractRoles("valid-token")).thenReturn(List.of("STUDENT"));
        when(jwtValidator.extractPermissions("valid-token")).thenReturn(List.of("COURSE_VIEW"));

        filter.filter(exchange, filterChain).block();

        verify(filterChain).filter(any());
        ServerHttpRequest downstream = capturedRequest.get();
        assertNotNull(downstream);
        assertEquals(userId, downstream.getHeaders().getFirst("X-User-Id"));
        assertEquals("learner@example.com", downstream.getHeaders().getFirst("X-User-Email"));
        assertEquals("STUDENT", downstream.getHeaders().getFirst("X-Roles"));
        assertEquals("COURSE_VIEW", downstream.getHeaders().getFirst("X-Perms"));
    }
}
