package com.agentbanking.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/admin")
public class AdminHealthController {

    private static final Logger log = LoggerFactory.getLogger(AdminHealthController.class);
    private static final int TIMEOUT_SECONDS = 3;
    private static final int TOTAL_TIMEOUT_SECONDS = 5;

    private static final Map<String, ServiceInfo> SERVICES = new LinkedHashMap<>() {{
        put("gateway", new ServiceInfo(8080, "External API entry point"));
        put("rules", new ServiceInfo(8081, "Transaction rules engine"));
        put("ledger", new ServiceInfo(8082, "Financial ledger & float"));
        put("onboarding", new ServiceInfo(8083, "Agent onboarding & KYC"));
        put("switch", new ServiceInfo(8084, "Payment network adapter"));
        put("biller", new ServiceInfo(8085, "Bill payment processing"));
        put("orchestrator", new ServiceInfo(8086, "Transaction Saga coordination"));
        put("auth", new ServiceInfo(8087, "Authentication & authorization"));
        put("audit", new ServiceInfo(8088, "Audit log aggregation"));
    }};

    private final ExecutorService executor;
    private final HttpClient httpClient;

    public AdminHealthController() {
        this.executor = Executors.newFixedThreadPool(10);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
    }

    @GetMapping("/health/all")
    public Map<String, Object> aggregateHealth() {
        log.debug("Starting parallel health check for all services");
        
        List<CompletableFuture<Map<String, Object>>> futures = SERVICES.entrySet().stream()
            .map(entry -> {
                String name = entry.getKey();
                ServiceInfo info = entry.getValue();
                
                return CompletableFuture.supplyAsync(() -> checkServiceHealth(name, info), executor)
                    .exceptionally(e -> {
                        log.warn("Health check failed for {}: {}", name, e.getMessage());
                        return Map.of(
                            "name", name, "port", info.port(), "purpose", info.purpose(),
                            "status", "DOWN", "lastChecked", Instant.now().toString(),
                            "error", e.getMessage()
                        );
                    });
            })
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Map<String, Object>> services = new ArrayList<>();
        for (CompletableFuture<Map<String, Object>> f : futures) {
            try {
                services.add(f.get(TOTAL_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("Timeout waiting for health result: {}", e.getMessage());
                services.add(Map.of("status", "UNKNOWN"));
            }
        }

        long healthy = services.stream().filter(s -> "UP".equals(s.get("status"))).count();
        long unhealthy = services.size() - healthy;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("services", services);
        response.put("summary", Map.of("total", services.size(), "healthy", healthy, "unhealthy", unhealthy));
        response.put("timestamp", Instant.now().toString());
        
        log.debug("Health check complete: {} healthy, {} unhealthy", healthy, unhealthy);
        return response;
    }

    private Map<String, Object> checkServiceHealth(String name, ServiceInfo info) {
        String host = switch (name) {
            case "gateway" -> "localhost";
            case "auth" -> "auth-iam-service";
            case "switch" -> "switch-adapter-service";
            default -> name + "-service";
        };
        try {
            String url = "http://" + host + ":" + info.port();
            URI uri = URI.create(url + "/actuator/health");
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri).timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET().build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String status = response.statusCode() == 200 ? "UP" : "DOWN";
            return Map.of(
                "name", name, "port", info.port(), "purpose", info.purpose(),
                "status", status, "lastChecked", Instant.now().toString()
            );
        } catch (java.net.http.HttpTimeoutException e) {
            return Map.of(
                "name", name, "port", info.port(), "purpose", info.purpose(),
                "status", "DEGRADED", "lastChecked", Instant.now().toString(),
                "error", "Timeout after " + TIMEOUT_SECONDS + "s"
            );
        } catch (Exception e) {
            return Map.of(
                "name", name, "port", info.port(), "purpose", info.purpose(),
                "status", "DOWN", "lastChecked", Instant.now().toString(),
                "error", e.getMessage()
            );
        }
    }

    private record ServiceInfo(int port, String purpose) {}
}