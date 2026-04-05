package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ServiceRouteGatewayFilterFactory
        extends AbstractGatewayFilterFactory<ServiceRouteGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(ServiceRouteGatewayFilterFactory.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> SERVICE_URLS = new HashMap<>() {{
        put("gateway", "http://gateway:8080");
        put("rules", "http://rules-service:8081");
        put("ledger", "http://ledger-service:8082");
        put("onboarding", "http://onboarding-service:8083");
        put("switch", "http://switch-adapter-service:8084");
        put("biller", "http://biller-service:8085");
        put("orchestrator", "http://orchestrator-service:8086");
        put("auth", "http://auth-iam-service:8087");
        put("audit", "http://audit-service:8088");
        put("mock", "http://mock-server:8089");
        put("postgresql", "direct");
        put("redis", "direct");
        put("kafka", "direct");
    }};

    public ServiceRouteGatewayFilterFactory() { super(Config.class); }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String servicePath = exchange.getRequest().getPath().value();
            String[] parts = servicePath.split("/");
            String serviceName = null;
            String actuatorPath = null;

            for (int i = 0; i < parts.length; i++) {
                if ("health".equals(parts[i]) && i + 1 < parts.length) {
                    serviceName = parts[i + 1];
                    actuatorPath = "/actuator/health";
                    break;
                } else if ("metrics".equals(parts[i]) && i + 1 < parts.length) {
                    serviceName = parts[i + 1];
                    actuatorPath = "/actuator/metrics";
                    break;
                }
            }

            if (serviceName == null || !SERVICE_URLS.containsKey(serviceName)) {
                return onError(exchange, "Unknown service: " + serviceName, "ERR_SYS_001");
            }

            String url = SERVICE_URLS.get(serviceName);
            if ("direct".equals(url)) {
                return onError(exchange, "Infrastructure component health check not supported via HTTP", "ERR_SYS_002");
            }

            String targetUrl = url + actuatorPath;
            log.debug("Routing to service {}: {}", serviceName, targetUrl);

            exchange.getAttributes().put(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                URI.create(targetUrl)
            );

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
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
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {}
}
