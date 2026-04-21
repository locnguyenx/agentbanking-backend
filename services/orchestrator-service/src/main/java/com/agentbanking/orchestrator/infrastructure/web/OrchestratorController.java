package com.agentbanking.orchestrator.infrastructure.web;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase.StartTransactionCommand;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase.StartTransactionResult;
import com.agentbanking.orchestrator.infrastructure.web.dto.*;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class OrchestratorController {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorController.class);

    private final StartTransactionUseCase startTransactionUseCase;
    private final QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;
    private final WorkflowFactory workflowFactory;

    public OrchestratorController(StartTransactionUseCase startTransactionUseCase,
                                   QueryWorkflowStatusUseCase queryWorkflowStatusUseCase,
                                   WorkflowFactory workflowFactory) {
        this.startTransactionUseCase = startTransactionUseCase;
        this.queryWorkflowStatusUseCase = queryWorkflowStatusUseCase;
        this.workflowFactory = workflowFactory;
    }

    @PostMapping
    public ResponseEntity<?> startTransaction(@Valid @RequestBody TransactionRequest request) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existing = queryWorkflowStatusUseCase.getStatus(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning cached response for idempotency key: {}", request.idempotencyKey());
                // BDD-IDE-02/03: Return 202 for cached responses too
                return ResponseEntity.accepted()
                        .location(java.net.URI.create("/api/v1/transactions/" + request.idempotencyKey() + "/status"))
                        .body(mapToTransactionResponse(
                            new StartTransactionResult("PENDING", request.idempotencyKey(),
                                "/api/v1/transactions/" + request.idempotencyKey() + "/status")));
            }
        }

        StartTransactionCommand command = new StartTransactionCommand(
            request.transactionType(),
            request.agentId(),
            request.amount(),
            request.idempotencyKey(),
            request.pan(),
            request.pinBlock(),
            request.customerCardMasked(),
            request.destinationAccount(),
            request.requiresBiometric(),
            request.billerCode(),
            request.ref1(),
            request.ref2(),
            request.proxyType(),
            request.proxyValue(),
            request.customerMykad(),
            request.geofenceLat(),
            request.geofenceLng(),
            request.targetBIN(),
            request.agentTier()
        );

        StartTransactionResult result = startTransactionUseCase.start(command);
        
        // BDD Spec: Return 202 Accepted for async workflow start
        // Response includes pollUrl for client to check status
        return ResponseEntity.accepted()
                .location(java.net.URI.create(result.pollUrl()))
                .body(mapToTransactionResponse(result));
    }

    @GetMapping("/{workflowId}/status")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String workflowId) {
        return queryWorkflowStatusUseCase.getStatus(workflowId)
            .map(status -> ResponseEntity.ok(mapToWorkflowStatusResponse(workflowId, status)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{workflowId}/force-resolve")
    public ResponseEntity<?> forceResolve(
            @PathVariable String workflowId,
            @Valid @RequestBody ForceResolveRequest request) {
        
        WorkflowStub workflowStub = workflowFactory.getWorkflowStub(workflowId);

        try {
            var signal = new com.agentbanking.orchestrator.domain.model.ForceResolveSignal(
                com.agentbanking.orchestrator.domain.model.ForceResolveSignal.Action.valueOf(
                    request.action().name()),
                request.reason()
            );
            workflowStub.signal("forceResolve", signal);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Force resolve signal sent for workflow: " + workflowId
            ));
        } catch (Exception e) {
            log.error("Failed to send force resolve signal for workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_SYS_FORCE_RESOLVE_FAILED",
                    "message", e.getMessage()
                )
            ));
        }
    }

    private TransactionResponse mapToTransactionResponse(StartTransactionResult result) {
        return new TransactionResponse(
            result.status(),
            result.workflowId(),
            result.pollUrl()
        );
    }

    private WorkflowStatusResponse mapToWorkflowStatusResponse(
            String workflowId,
            QueryWorkflowStatusUseCase.WorkflowStatusResponse status) {
        var result = status.result();
        var metadata = result != null ? result.metadata() : Map.<String, Object>of();
        
        // Convert errorDetails string to JsonNode if possible, or leave as null
        com.fasterxml.jackson.databind.JsonNode detailsNode = null;
        if (result != null && result.errorDetails() != null) {
            try {
                detailsNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.errorDetails());
            } catch (Exception e) {
                // If it's not JSON, we could wrap it in a TextNode but for now just leave null or handle as needed
            }
        }

        return new WorkflowStatusResponse(
            status.status() != null ? status.status().name() : "UNKNOWN",
            result != null ? result.pendingReason() : null,
            detailsNode,
            workflowId,
            null, // transactionType not currently in WorkflowResult
            toBigDecimal(result != null ? result.amount() : null),
            toBigDecimal(result != null ? result.customerFee() : null),
            result != null && result.referenceNumber() != null ? result.referenceNumber() : "",
            result != null ? result.errorCode() : null,
            result != null ? result.errorMessage() : null,
            result != null ? result.actionCode() : null,
            result != null ? result.completedAt() : null,
            safeToString(metadata.get("agentTier")),
            safeToString(metadata.get("targetBin")),
            safeToString(metadata.get("customerCardMasked")),
            toBigDecimal(metadata.get("geofenceLat")),
            toBigDecimal(metadata.get("geofenceLng")),
            safeToString(metadata.get("billerCode")),
            safeToString(metadata.get("ref1")),
            safeToString(metadata.get("ref2")),
            safeToString(metadata.get("destinationAccount"))
        );
    }

    private String safeToString(Object value) {
        return value != null ? value.toString() : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    public record TransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull UUID agentId,
        @NotNull BigDecimal amount,
        String idempotencyKey,
        String pan,
        String pinBlock,
        String customerCardMasked,
        String destinationAccount,
        boolean requiresBiometric,
        String billerCode,
        String ref1,
        String ref2,
        String proxyType,
        String proxyValue,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String targetBIN,
        String agentTier
    ) {}
}
