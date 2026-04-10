package com.agentbanking.gateway.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class OpenApiAggregatorController {

    private final WebClient webClient = WebClient.builder().build();

    // Only services exposed externally (internal services like rules-service are NOT exposed)
    private static final Map<String, String> SERVICE_URLS = Map.of(
        "ledger-service", "http://ledger-service:8082",
        "onboarding-service", "http://onboarding-service:8083",
        "switch-adapter-service", "http://switch-adapter-service:8084",
        "biller-service", "http://biller-service:8085"
    );

    // Internal path → External path mapping (from design doc)
    // Maps what services expose internally to what Gateway exposes externally
    private static final Map<String, String> PATH_MAPPING = new HashMap<>() {{
        // Ledger Service
        put("/internal/debit", "/api/v1/withdrawal");
        put("/internal/credit", "/api/v1/deposit");
        put("/internal/balance-inquiry", "/api/v1/balance-inquiry");
        put("/internal/balance", "/api/v1/agent/balance");
        put("/internal/backoffice/dashboard", "/api/v1/backoffice/dashboard");
        put("/internal/backoffice/transactions", "/api/v1/backoffice/transactions");
        put("/internal/backoffice/settlement", "/api/v1/backoffice/settlement");
        put("/internal/backoffice/ledger-transactions", "/api/v1/backoffice/ledger-transactions");

        // Transaction Service (Orchestrator)
        put("/internal/transactions/quote", "/api/v1/transactions/quote");
        put("/internal/transactions", "/api/v1/transactions");
        put("/internal/transactions/{workflowId}/status", "/api/v1/transactions/{workflowId}/status");
        put("/internal/transactions/{workflowId}/force-resolve", "/api/v1/transactions/{workflowId}/force-resolve");

        // Transfer / Switch Adapter
        put("/internal/transfer/proxy/enquiry", "/api/v1/transfer/proxy/enquiry");
        put("/internal/duitnow", "/api/v1/transfer/duitnow");
        put("/internal/compliance/status", "/api/v1/compliance/status");

        // Onboarding Service
        put("/internal/verify-mykad", "/api/v1/kyc/verify");
        put("/internal/biometric", "/api/v1/kyc/biometric");
        put("/internal/submit-application", "/api/v1/onboarding/submit-application");
        put("/backoffice/agents", "/api/v1/backoffice/agents");
        put("/backoffice/agents/{id}", "/api/v1/backoffice/agents/{id}");
        put("/internal/kyc/review-queue", "/api/v1/backoffice/kyc/review-queue");
        put("/internal/audit-logs", "/api/v1/backoffice/audit-logs");
        put("/backoffice/agents/{agentId}/user-status", "/api/v1/backoffice/agents/{agentId}/user-status");
        put("/backoffice/agents/{agentId}/create-user", "/api/v1/backoffice/agents/{agentId}/create-user");

        // Biller Service
        put("/internal/pay-bill", "/api/v1/bill/pay");
        put("/internal/billpayment/jompay", "/api/v1/billpayment/jompay");
        put("/internal/topup", "/api/v1/topup");
        put("/internal/ewallet/withdrawal", "/api/v1/ewallet/withdraw");
        put("/internal/ewallet/topup", "/api/v1/ewallet/topup");
        put("/internal/essp/purchase", "/api/v1/essp/purchase");

        // Merchant (Ledger Service)
        put("/internal/merchant/retail-sale", "/api/v1/retail/sale");
        put("/internal/merchant/pin-purchase", "/api/v1/retail/pin-purchase");
        put("/internal/merchant/cash-back", "/api/v1/retail/cashback");

        // Reconciliation (Ledger Service)
        put("/internal/reconciliation/discrepancy/maker-propose", "/api/v1/backoffice/discrepancy/{caseId}/maker-action");
        put("/internal/reconciliation/discrepancy/checker-approve", "/api/v1/backoffice/discrepancy/{caseId}/checker-approve");
        put("/internal/reconciliation/discrepancy/checker-reject", "/api/v1/backoffice/discrepancy/{caseId}/checker-reject");

        // Auth‑IAM Service
        put("/internal/auth/me", "/api/v1/auth/me");
        put("/internal/auth/token", "/api/v1/auth/token");
        put("/internal/auth/refresh", "/api/v1/auth/refresh");
        put("/internal/auth/revoke", "/api/v1/auth/revoke");
        put("/internal/auth/password/forgot", "/api/v1/auth/password/forgot");
        put("/internal/auth/password/reset", "/api/v1/auth/password/reset");
        put("/internal/auth/password/change", "/api/v1/auth/password/change");
    }};

    @GetMapping("/v3/api-docs")
    public Mono<Map<String, Object>> getAggregatedApiDocs() {
        return getApiDocs();
    }

    @GetMapping("/v3/api-docs/")
    public Mono<Map<String, Object>> getAggregatedApiDocsTrailingSlash() {
        return getApiDocs();
    }

    private Mono<Map<String, Object>> getApiDocs() {
        return Flux.fromIterable(SERVICE_URLS.entrySet())
            .flatMap(entry -> fetchServiceSpec(entry.getKey(), entry.getValue()))
            .collectList()
            .map(this::mergeSpecs);
    }

    private Mono<Map<String, Object>> fetchServiceSpec(String serviceName, String baseUrl) {
        return webClient.get()
            .uri(baseUrl + "/v3/api-docs")
            .retrieve()
            .bodyToMono(Map.class)
            .map(spec -> {
                Map<String, Object> result = new HashMap<>();
                result.put("serviceName", serviceName);
                result.put("spec", spec);
                return result;
            })
            .onErrorResume(e -> {
                Map<String, Object> result = new HashMap<>();
                result.put("serviceName", serviceName);
                result.put("spec", Map.of("paths", Map.of(), "components", Map.of("schemas", Map.of())));
                return Mono.just(result);
            });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeSpecs(List<Map<String, Object>> serviceSpecs) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("openapi", "3.0.3");
        merged.put("info", Map.of(
            "title", "Agent Banking Platform API",
            "description", "Aggregated API from all microservices",
            "version", "1.0.0",
            "contact", Map.of("name", "Agent Banking Platform Team")
        ));
        merged.put("servers", List.of(
            Map.of("url", "http://localhost:8080", "description", "Gateway"),
            Map.of("url", "https://api.agentbanking.example.com", "description", "Production"),
            Map.of("url", "https://sandbox.agentbanking.example.com", "description", "Sandbox")
        ));

        Map<String, Object> allPaths = new LinkedHashMap<>();
        Map<String, Object> allSchemas = new LinkedHashMap<>();
        List<Tag> allTags = new ArrayList<>();

        for (Map<String, Object> serviceEntry : serviceSpecs) {
            Map<String, Object> spec = (Map<String, Object>) serviceEntry.get("spec");
            if (spec == null) continue;

            // Merge paths
            Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
            if (paths != null) {
                for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                    String internalPath = pathEntry.getKey();
                    Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
                    
                    // Map internal path to external path
                    String externalPath = PATH_MAPPING.getOrDefault(internalPath, null);
                    
                    // Skip unmapped internal paths
                    if (externalPath == null) {
                        // Try pattern matching for paths with parameters
                        if (internalPath.startsWith("/internal/backoffice/agents/")) {
                            externalPath = "/api/v1/backoffice/agents/{id}";
                        } else if (internalPath.startsWith("/internal/backoffice/settlement/")) {
                            externalPath = "/api/v1/backoffice/settlement/{agentId}";
                        } else {
                            // Skip unknown internal paths
                            continue;
                        }
                    }
                    
                    // Update tags to include service name
                    Map<String, Object> updatedPathItem = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                        String method = methodEntry.getKey();
                        Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                        
                        if (operation.containsKey("tags")) {
                            List<String> tags = (List<String>) operation.get("tags");
                            tags = tags.stream()
                                .map(tag -> tag + " (" + serviceEntry.get("serviceName") + ")")
                                .collect(Collectors.toList());
                            operation.put("tags", tags);
                        }
                        
                        updatedPathItem.put(method, operation);
                    }
                    
                    allPaths.put(externalPath, updatedPathItem);
                }
            }

            // Merge schemas
            Map<String, Object> components = (Map<String, Object>) spec.get("components");
            if (components != null) {
                Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
                if (schemas != null) {
                    allSchemas.putAll(schemas);
                }
            }

            // Merge tags
            List<Map<String, Object>> tags = (List<Map<String, Object>>) spec.get("tags");
            if (tags != null) {
                for (Map<String, Object> tag : tags) {
                    allTags.add(new Tag()
                        .name((String) tag.get("name"))
                        .description((String) tag.get("description")));
                }
            }
        }

        merged.put("paths", allPaths);
        merged.put("components", Map.of("schemas", allSchemas));
        merged.put("tags", allTags.stream()
            .map(t -> Map.of("name", t.getName(), "description", t.getDescription() != null ? t.getDescription() : ""))
            .collect(Collectors.toList()));

        return merged;
    }
}
