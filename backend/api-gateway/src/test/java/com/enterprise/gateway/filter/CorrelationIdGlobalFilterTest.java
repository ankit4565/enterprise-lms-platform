package com.enterprise.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationIdGlobalFilterTest {

    private CorrelationIdGlobalFilter filter;
    private GatewayFilterChain filterChain;
    private AtomicReference<ServerHttpRequest> capturedRequest;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdGlobalFilter();
        filterChain = mock(GatewayFilterChain.class);
        capturedRequest = new AtomicReference<>();

        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            capturedRequest.set(exchange.getRequest());
            return Mono.empty();
        });
    }

    @Test
    void filter_WhenCorrelationIdAbsent_GeneratesNewOne() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, filterChain).block();

        ServerHttpRequest downstreamRequest = capturedRequest.get();
        assertNotNull(downstreamRequest);
        String correlationId = downstreamRequest.getHeaders().getFirst("X-Correlation-Id");
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
    }

    @Test
    void filter_WhenCorrelationIdPresent_PreservesExistingOne() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .header("X-Correlation-Id", "existing-id-12345")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, filterChain).block();

        ServerHttpRequest downstreamRequest = capturedRequest.get();
        assertNotNull(downstreamRequest);
        assertEquals("existing-id-12345", downstreamRequest.getHeaders().getFirst("X-Correlation-Id"));
    }
}
