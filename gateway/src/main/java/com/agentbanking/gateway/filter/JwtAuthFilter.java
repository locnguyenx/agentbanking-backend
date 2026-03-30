package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReactiveJwtDecoder reactiveJwtDecoder;

    public JwtAuthFilter(@Value("${spring.security.oauth2.resourceserver.jwt.secret:}") String jwtSecret) {
        super(Config.class);
        System.out.println("=== JWT DEBUG: jwtSecret = '" + jwtSecret + "' (length: " + (jwtSecret != null ? jwtSecret.length() : 0) + ") ===");
        System.out.println("=== JWT DEBUG: JwtAuthFilter instance created ===");
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            this.reactiveJwtDecoder = token -> {
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith((javax.crypto.SecretKey) key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    System.out.println("=== JWT DEBUG: Token parsed successfully, subject: " + claims.getSubject() + " ===");
                    return Mono.just(Jwt.withTokenValue(token)
                            .header("alg", "HS512")
                            .subject(claims.getSubject())
                            .claim("agent_id", claims.get("agent_id"))
                            .claim("permissions", claims.get("permissions"))
                            .issuedAt(Instant.ofEpochSecond(claims.getIssuedAt().getTime() / 1000))
                            .expiresAt(Instant.ofEpochSecond(claims.getExpiration().getTime() / 1000))
                            .build());
                } catch (Exception e) {
                    System.out.println("=== JWT DEBUG: Token validation FAILED: " + e.getMessage() + " ===");
                    return Mono.error(new RuntimeException("Invalid JWT: " + e.getMessage()));
                }
            };
        } else {
            this.reactiveJwtDecoder = token -> Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "HS512")
                    .subject("dummy")
                    .build());
        }
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

            String token = authHeader.substring(7);
            System.out.println("=== JWT DEBUG: Attempting to validate token ===");
            System.out.println("=== JWT DEBUG: Token prefix: " + token.substring(0, Math.min(20, token.length())) + " ===");

            return reactiveJwtDecoder.decode(token)
                .doOnSubscribe(s -> System.out.println("=== JWT DEBUG: Subscribed to decoder ==="))
                .doOnError(e -> System.out.println("=== JWT DEBUG: Decoder error: " + e.getMessage() + " ==="))
                .flatMap(jwt -> {
                    String agentIdClaim = jwt.getClaimAsString("agent_id");
                    String agentId = (agentIdClaim != null && !agentIdClaim.isBlank()) 
                        ? agentIdClaim 
                        : jwt.getSubject();
                    final String agentTier = jwt.getClaimAsString("agent_tier");

                    if (agentId == null || agentId.isBlank()) {
                        return onError(exchange, "Missing agent_id claim in token", "ERR_AUTH_INVALID_TOKEN");
                    }

                    var exchangeBuilder = exchange.mutate();
                    exchangeBuilder.request(r -> r.header("X-Agent-Id", agentId));

                    if (agentTier != null && !agentTier.isBlank()) {
                        exchangeBuilder.request(r -> r.header("X-Agent-Tier", agentTier));
                    }

                    ServerWebExchange modifiedExchange = exchangeBuilder.build();
                    System.out.println("=== JWT DEBUG: Token validated successfully, forwarding to backend ===");

                    return chain.filter(modifiedExchange);
                })
                .onErrorResume(e -> onError(exchange, "Invalid token: " + e.getMessage(), "ERR_AUTH_INVALID_TOKEN"));
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