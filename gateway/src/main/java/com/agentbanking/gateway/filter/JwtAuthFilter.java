package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", "ERR_AUTH_MISSING_TOKEN");
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", "ERR_AUTH_INVALID_TOKEN");
            }

            return ReactiveSecurityContextHolder.getContext()
                .filter(ctx -> ctx.getAuthentication() != null)
                .map(ctx -> (Jwt) ctx.getAuthentication().getPrincipal())
                .flatMap(jwt -> {
                    String agentId = jwt.getClaimAsString("agent_id");
                    if (agentId == null) {
                        agentId = jwt.getSubject();
                    }
                    String agentTier = jwt.getClaimAsString("agent_tier");

                    if (agentId == null || agentId.isBlank()) {
                        return onError(exchange, "Missing agent_id claim in token", "ERR_AUTH_INVALID_TOKEN");
                    }

                    ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(r -> r.header("X-Agent-Id", agentId))
                        .build();

                    if (agentTier != null && !agentTier.isBlank()) {
                        modifiedExchange = modifiedExchange.mutate()
                            .request(r -> r.header("X-Agent-Tier", agentTier))
                            .build();
                    }

                    return chain.filter(modifiedExchange);
                })
                .switchIfEmpty(Mono.defer(() -> onError(exchange, "Invalid token", "ERR_AUTH_INVALID_TOKEN")));
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String timestamp = Instant.now().toString();
        Map<String, Object> errorResponse = Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", errorCode,
                "message", message,
                "action_code", "DECLINE",
                "trace_id", UUID.randomUUID().toString(),
                "timestamp", timestamp
            )
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {
    }
}