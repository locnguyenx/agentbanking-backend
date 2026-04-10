package com.agentbanking.audit.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthAggregationService {

    private static final Logger log = LoggerFactory.getLogger(HealthAggregationService.class);
    private static final int TIMEOUT_SECONDS = 3;
    private static final int PARALLEL_TIMEOUT_SECONDS = 5;

    private final Map<String, String> serviceUrls;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ExecutorService executor;

    public HealthAggregationService(Map<String, String> serviceUrls) {
        this.serviceUrls = serviceUrls;
        this.executor = Executors.newFixedThreadPool(Math.min(serviceUrls.size(), 10));
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build()
        );
    }

    public Map<String, Object> aggregateHealth() {
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);

            CompletableFuture<Map<String, Object>> future = CompletableFuture
                .supplyAsync(() -> checkServiceHealth(name, url, cb), executor)
                .exceptionally(e -> {
                    log.warn("Health check failed for {}: {}", name, e.getMessage());
                    return Map.of(
                        "name", name, "status", "DOWN",
                        "lastChecked", Instant.now().toString(),
                        "error", e.getMessage()
                    );
                });
            futures.add(future);
        }

        List<Map<String, Object>> services = new ArrayList<>();
        int healthy = 0;
        int unhealthy = 0;

        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> healthInfo = future.get(PARALLEL_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                String status = (String) healthInfo.get("status");
                if ("UP".equals(status)) healthy++; else unhealthy++;
                services.add(healthInfo);
            } catch (Exception e) {
                log.warn("Timeout or error waiting for health result: {}", e.getMessage());
                unhealthy++;
            }
        }

        return Map.of(
            "services", services,
            "summary", Map.of("total", services.size(), "healthy", healthy, "unhealthy", unhealthy),
            "timestamp", Instant.now().toString()
        );
    }

    private Map<String, Object> checkServiceHealth(String name, String url, CircuitBreaker cb) {
        return cb.executeSupplier(() -> {
            try {
                java.net.URI uri = java.net.URI.create(url + "/actuator/health");
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri).timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).GET().build();
                java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                String status = response.statusCode() == 200 ? "UP" : "DOWN";
                return Map.of(
                    "name", name, "port", extractPort(url),
                    "purpose", getServicePurpose(name), "status", status,
                    "lastChecked", Instant.now().toString()
                );
            } catch (java.net.http.HttpTimeoutException e) {
                return Map.of(
                    "name", name, "status", "DEGRADED",
                    "lastChecked", Instant.now().toString(),
                    "error", "Timeout after " + TIMEOUT_SECONDS + "s"
                );
            } catch (Exception e) {
                throw new RuntimeException("Health check failed: " + e.getMessage(), e);
            }
        });
    }

    private int extractPort(String url) {
        try { return Integer.parseInt(url.substring(url.lastIndexOf(':') + 1)); }
        catch (Exception e) { return 0; }
    }

    private String getServicePurpose(String name) {
        return switch (name) {
            case "gateway" -> "External API entry point";
            case "rules" -> "Transaction rules engine";
            case "ledger" -> "Financial ledger & float";
            case "onboarding" -> "Agent onboarding & KYC";
            case "switch" -> "Payment network adapter";
            case "biller" -> "Bill payment processing";
            case "orchestrator" -> "Transaction Saga coordination";
            case "auth" -> "Authentication & authorization";
            case "audit" -> "Audit log aggregation";
            case "mock" -> "Development/testing";
            default -> "Unknown service";
        };
    }
}
