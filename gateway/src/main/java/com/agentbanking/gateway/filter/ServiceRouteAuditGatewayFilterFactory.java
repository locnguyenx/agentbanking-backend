package com.agentbanking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ServiceRouteAuditGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ServiceRouteAuditGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(ServiceRouteAuditGatewayFilterFactory.class);

    private static final Map<String, ServiceConfig> SERVICES = Map.of(
        "gateway", new ServiceConfig("http://gateway:8080", "/gateway/audit/logs"),
        "rules", new ServiceConfig("http://rules-service:8081", "/internal/audit-logs"),
        "ledger", new ServiceConfig("http://ledger-service:8082", "/internal/audit-logs"),
        "onboarding", new ServiceConfig("http://onboarding-service:8083", "/internal/audit-logs"),
        "switch", new ServiceConfig("http://switch-adapter-service:8084", "/internal/audit-logs"),
        "biller", new ServiceConfig("http://biller-service:8085", "/internal/audit-logs"),
        "orchestrator", new ServiceConfig("http://orchestrator-service:8086", "/internal/audit-logs"),
        "auth", new ServiceConfig("http://auth-iam-service:8087", "/auth/audit/logs"),
        "audit", new ServiceConfig("http://audit-service:8088", "/audit/logs")
    );

    public ServiceRouteAuditGatewayFilterFactory() {
        super(Config.class);
        log.info("ServiceRouteAuditGatewayFilterFactory initialized");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var uri = exchange.getRequest().getURI();
            var queryParams = uri.getQuery();
            String serviceName = null;
            
            log.info("ServiceRouteAuditFilter: uri={}, query={}", uri, queryParams);
            
            if (queryParams != null) {
                for (String param : queryParams.split("&")) {
                    if (param.startsWith("service=")) {
                        serviceName = param.substring("service=".length());
                        break;
                    }
                }
            }

            if (serviceName == null || serviceName.isBlank()) {
                log.warn("ServiceRouteAuditFilter: Missing service param");
                return onError(exchange, "Missing 'service' query parameter (e.g., ?service=auth)", "ERR_SYS_003");
            }

            ServiceConfig serviceConfig = SERVICES.get(serviceName.toLowerCase());
            if (serviceConfig == null) {
                log.warn("ServiceRouteAuditFilter: Unknown service {}", serviceName);
                return onError(exchange, "Unknown service: " + serviceName + ". Valid: " + SERVICES.keySet(), "ERR_SYS_001");
            }

            String path = uri.getPath();
            String targetPath = serviceConfig.auditPath;
            
            // Handle export vs query
            if (path.contains("/export")) {
                targetPath = serviceConfig.auditPath + "/export";
            }
            
            String fullUrl = serviceConfig.baseUrl + targetPath;
            if (queryParams != null) {
                // Remove 'service' param before forwarding (already used for routing)
                String filteredParams = java.util.stream.Stream.of(queryParams.split("&"))
                    .filter(p -> !p.startsWith("service="))
                    .collect(Collectors.joining("&"));
                if (!filteredParams.isEmpty()) {
                    fullUrl += "?" + filteredParams;
                }
            }

            log.info("ServiceRouteAuditFilter: Routing {} to {}", serviceName, fullUrl);

            URI newUri = URI.create(fullUrl);
            exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                newUri
            );

            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate().uri(newUri).build())
                .build();

            return chain.filter(mutatedExchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> errorResponse = Map.of(
            "status", "FAILED",
            "error", Map.of(
                "code", errorCode, "message", message,
                "action_code", "REVIEW",
                "trace_id", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString()
            )
        );
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    private record ServiceConfig(String baseUrl, String auditPath) {}
    public static class Config {}
}