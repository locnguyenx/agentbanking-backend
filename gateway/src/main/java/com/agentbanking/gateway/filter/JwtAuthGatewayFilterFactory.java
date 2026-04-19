package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Authentication filter factory for Spring Cloud Gateway.
 * 
 * Naming convention: Class is JwtAuthGatewayFilterFactory
 * → Referenced in YAML as: JwtAuth
 * 
 * Example in application.yaml:
 *   filters:
 *     - JwtAuth
 */
@Component
public class JwtAuthGatewayFilterFactory 
        extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilterFactory.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Key signingKey;

    public JwtAuthGatewayFilterFactory(
            @Value("${jwt.secret:${spring.security.oauth2.resourceserver.jwt.secret:}}") String jwtSecret) {
        super(Config.class);
        log.info("JwtAuthGatewayFilterFactory initialized, secret length: {}", 
                jwtSecret != null ? jwtSecret.length() : 0);
        
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    @Override
    public GatewayFilter apply(Config config) {
        // Order 0 ensures this runs before other filters
        return new OrderedGatewayFilter((exchange, chain) -> {
            // Check for Authorization header
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", "ERR_AUTH_MISSING_TOKEN");
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", "ERR_AUTH_INVALID_TOKEN");
            }

            String token = authHeader.substring(7);
            log.debug("Validating JWT token");

            // Validate token and extract claims
            try {
                Claims claims = Jwts.parser()
                        .verifyWith((javax.crypto.SecretKey) signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Extract agent_id from claims
                String agentId = claims.get("agent_id", String.class);


                if (agentId == null || agentId.isBlank()) {
                    return onError(exchange, "Missing agent_id claim in token", "ERR_AUTH_INVALID_TOKEN");
                }

                // Check token expiration
                if (claims.getExpiration().before(java.util.Date.from(Instant.now()))) {
                    return onError(exchange, "Token expired", "ERR_AUTH_TOKEN_EXPIRED");
                }

                // Set headers for downstream services
                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Agent-Id", agentId)
                        .header("X-User-Id", claims.getSubject());

                String agentTier = claims.get("agent_tier", String.class);
                if (agentTier != null && !agentTier.isBlank()) {
                    mutatedRequest.header("X-Agent-Tier", agentTier);
                }

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(mutatedRequest.build())
                        .build();

                log.debug("Token validated, agentId: {}", agentId);
                return chain.filter(modifiedExchange);

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("Token expired: {}", e.getMessage());
                return onError(exchange, "Token expired", "ERR_AUTH_TOKEN_EXPIRED");
            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid token: " + e.getMessage(), "ERR_AUTH_INVALID_TOKEN");
            }
        }, Ordered.HIGHEST_PRECEDENCE);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", errorCode,
                "message", message,
                "action_code", "DECLINE",
                "trace_id", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString()
            )
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
