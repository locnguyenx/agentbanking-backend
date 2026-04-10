package com.agentbanking.orchestrator.infrastructure.web;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.in.ApproveResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.ProposeResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.RejectResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import com.agentbanking.orchestrator.infrastructure.web.dto.CheckerActionRequest;
import com.agentbanking.orchestrator.infrastructure.web.dto.ForceResolveRequest;
import com.agentbanking.orchestrator.infrastructure.web.dto.MakerProposalRequest;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/transactions")
public class ResolutionController {

    private static final Logger log = LoggerFactory.getLogger(ResolutionController.class);

    private final ProposeResolutionUseCase proposeResolutionUseCase;
    private final ApproveResolutionUseCase approveResolutionUseCase;
    private final RejectResolutionUseCase rejectResolutionUseCase;
    private final ResolutionService resolutionService;
    private final TransactionRecordRepository transactionRecordRepository;
    private final WorkflowFactory workflowFactory;

    public ResolutionController(ProposeResolutionUseCase proposeResolutionUseCase,
                                 ApproveResolutionUseCase approveResolutionUseCase,
                                 RejectResolutionUseCase rejectResolutionUseCase,
                                 ResolutionService resolutionService,
                                 TransactionRecordRepository transactionRecordRepository,
                                 WorkflowFactory workflowFactory) {
        this.proposeResolutionUseCase = proposeResolutionUseCase;
        this.approveResolutionUseCase = approveResolutionUseCase;
        this.rejectResolutionUseCase = rejectResolutionUseCase;
        this.resolutionService = resolutionService;
        this.transactionRecordRepository = transactionRecordRepository;
        this.workflowFactory = workflowFactory;
    }

    @PostMapping("/{workflowId}/create-case")
    public ResponseEntity<?> createResolutionCase(
            @PathVariable UUID workflowId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Creating resolution case for workflow: {}, user: {}", workflowId, userId);
        
        try {
            var transactionRecord = transactionRecordRepository.findByWorkflowId(workflowId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for workflow: " + workflowId));
            
            var existingCase = resolutionService.findByWorkflowId(workflowId);
            if (existingCase.isPresent()) {
                var case_ = existingCase.get();
                log.info("Resolution case already exists for workflow: {}", workflowId);
                return ResponseEntity.ok(mapToResolutionResponse(case_));
            }
            
            var newCase = resolutionService.createPendingCase(workflowId, transactionRecord.id());
            log.info("Created new resolution case: {} for workflow: {}", newCase.id(), workflowId);
            return ResponseEntity.ok(mapToResolutionResponse(newCase));
        } catch (IllegalArgumentException e) {
            log.error("Failed to create resolution case: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_BIZ_RESOLUTION_FAILED", e.getMessage(), "RETRY"));
        }
    }

    @PostMapping("/{workflowId}/maker-propose")
    public ResponseEntity<?> makerPropose(
            @PathVariable UUID workflowId,
            @Valid @RequestBody MakerProposalRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Maker proposing resolution for workflow: {}, user: {}", workflowId, userId);
        
        try {
            var command = new ProposeResolutionUseCase.Command(
                workflowId,
                request.action(),
                userId,
                request.reasonCode(),
                request.reason(),
                request.evidenceUrl()
            );
            
            var result = proposeResolutionUseCase.propose(command);
            return ResponseEntity.ok(mapToResolutionResponse(result));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to propose resolution: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_BIZ_RESOLUTION_FAILED", e.getMessage(), "RETRY"));
        }
    }

    @PostMapping("/{workflowId}/checker-approve")
    public ResponseEntity<?> checkerApprove(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CheckerActionRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Checker approving resolution for workflow: {}, user: {}", workflowId, userId);
        
        try {
            var command = new ApproveResolutionUseCase.Command(
                workflowId,
                userId,
                request.reason()
            );
            
            var result = approveResolutionUseCase.approve(command);
            return ResponseEntity.ok(mapToResolutionResponse(result));
        } catch (SecurityException e) {
            log.error("Four-eyes violation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_AUTH_SELF_APPROVAL", e.getMessage(), "DECLINE"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to approve resolution: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_BIZ_RESOLUTION_FAILED", e.getMessage(), "RETRY"));
        }
    }

    @PostMapping("/{workflowId}/checker-reject")
    public ResponseEntity<?> checkerReject(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CheckerActionRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Checker rejecting resolution for workflow: {}, user: {}", workflowId, userId);
        
        try {
            var command = new RejectResolutionUseCase.Command(
                workflowId,
                userId,
                request.reason()
            );
            
            var result = rejectResolutionUseCase.reject(command);
            return ResponseEntity.ok(mapToResolutionResponse(result));
        } catch (SecurityException e) {
            log.error("Four-eyes violation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_AUTH_SELF_APPROVAL", e.getMessage(), "DECLINE"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to reject resolution: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse("ERR_BIZ_RESOLUTION_FAILED", e.getMessage(), "RETRY"));
        }
    }

    @GetMapping("/resolutions")
    public ResponseEntity<?> listResolutions(
            @RequestParam(required = false) String status) {
        
        List<TransactionResolutionCase> results;
        
        if (status != null && !status.isBlank()) {
            var resolutionStatus = ResolutionStatus.valueOf(status.toUpperCase());
            results = resolutionService.findByStatus(resolutionStatus);
        } else {
            results = resolutionService.findAll();
        }
        
        var content = results.stream()
            .map(this::mapToResolutionResponse)
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("total", content.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listTransactions(
            @RequestParam(required = false) String status) {
        
        log.info("Fetching transactions with resolution cases, status filter: {}", status);
        
        List<TransactionResolutionCase> cases;
        if (status != null && !status.isBlank()) {
            var resolutionStatus = ResolutionStatus.valueOf(status.toUpperCase());
            cases = resolutionService.findByStatus(resolutionStatus);
        } else {
            cases = resolutionService.findAll();
        }
        
        var content = cases.stream()
            .map(c -> {
                var txRecord = transactionRecordRepository.findByWorkflowId(c.workflowId().toString());
                
                Map<String, Object> map = new HashMap<>();
                map.put("caseId", c.id());
                map.put("workflowId", c.workflowId());
                map.put("transactionId", c.transactionId() != null ? c.transactionId().toString() : "");
                map.put("transactionType", txRecord.isPresent() && txRecord.get().transactionType() != null ? txRecord.get().transactionType().name() : "");
                map.put("agentId", txRecord.isPresent() && txRecord.get().agentId() != null ? txRecord.get().agentId().toString() : "");
                map.put("amount", txRecord.isPresent() ? txRecord.get().amount() : null);
                map.put("status", c.status().name());
                map.put("proposedAction", c.proposedAction() != null ? c.proposedAction().name() : "");
                map.put("reasonCode", c.reasonCode() != null ? c.reasonCode() : "");
                map.put("reason", c.reason() != null ? c.reason() : "");
                map.put("makerUserId", c.makerUserId() != null ? c.makerUserId() : "");
                map.put("makerCreatedAt", c.makerCreatedAt() != null ? c.makerCreatedAt().toString() : "");
                map.put("checkerUserId", c.checkerUserId() != null ? c.checkerUserId() : "");
                map.put("checkerAction", c.checkerAction() != null ? c.checkerAction() : "");
                map.put("checkerReason", c.checkerReason() != null ? c.checkerReason() : "");
                map.put("checkerCompletedAt", c.checkerCompletedAt() != null ? c.checkerCompletedAt().toString() : "");
                map.put("makerPendingReason", c.makerPendingReason() != null ? c.makerPendingReason() : "");
                map.put("checkerPendingReason", c.checkerPendingReason() != null ? c.checkerPendingReason() : "");
                map.put("createdAt", c.createdAt() != null ? c.createdAt().toString() : "");
                map.put("updatedAt", c.updatedAt() != null ? c.updatedAt().toString() : "");
                
if (txRecord.isPresent()) {
                     map.put("errorCode", txRecord.get().errorCode() != null ? txRecord.get().errorCode() : "");
                     map.put("errorMessage", txRecord.get().errorMessage() != null ? txRecord.get().errorMessage() : "");
                     map.put("completedAt", txRecord.get().completedAt() != null ? txRecord.get().completedAt().toString() : "");
                     map.put("referenceNumber", txRecord.get().referenceNumber() != null ? txRecord.get().referenceNumber() : "");
                     map.put("customerFee", txRecord.get().customerFee() != null ? txRecord.get().customerFee() : null);
                 }
                
                return map;
            })
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("total", content.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflows")
    public ResponseEntity<?> getWorkflows(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) String agentCode,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching workflows with filters: fromDate={}, toDate={}, agentId={}, transactionType={}, status={}",
                fromDate, toDate, agentId, transactionType, status);

        try {
            Instant from = fromDate != null ? Instant.parse(fromDate) : null;
            Instant to = toDate != null ? Instant.parse(toDate) : null;

            var workflows = transactionRecordRepository.findAllWithFilters(
                    from, to, agentId, agentCode, transactionType, status, page, size);

            var content = workflows.stream()
                .map(w -> {
                    Map<String, Object> map = new HashMap<>();
map.put("workflowId", w.workflowId());
                     map.put("transactionId", w.id() != null ? w.id().toString() : "");
                     map.put("transactionType", w.transactionType() != null ? w.transactionType().name() : "");
                     map.put("agentId", w.agentId() != null ? w.agentId().toString() : "");
                     map.put("amount", w.amount());
                     map.put("status", w.status());
                     map.put("createdAt", w.createdAt() != null ? w.createdAt().toString() : "");
                     map.put("completedAt", w.completedAt() != null ? w.completedAt().toString() : "");
                    map.put("referenceNumber", w.referenceNumber() != null ? w.referenceNumber() : "");
                      map.put("customerFee", w.customerFee() != null ? w.customerFee() : null);
                      map.put("pendingReason", w.pendingReason() != null ? w.pendingReason() : "");
                      return map;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            response.put("page", page);
            response.put("size", size);
            response.put("total", content.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching workflows: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/stuck")
    public ResponseEntity<?> getStuckTransactions() {
        log.info("Fetching stuck transactions");
        var stuckTransactions = transactionRecordRepository.findStuckTransactions();
        var content = stuckTransactions.stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("workflowId", t.workflowId());
                map.put("transactionId", t.id() != null ? t.id().toString() : "");
                map.put("transactionType", t.transactionType() != null ? t.transactionType().name() : "");
                map.put("agentId", t.agentId() != null ? t.agentId().toString() : "");
                map.put("amount", t.amount());
                map.put("status", t.status());
                map.put("errorCode", t.errorCode() != null ? t.errorCode() : "");
                map.put("errorMessage", t.errorMessage() != null ? t.errorMessage() : "");
                map.put("createdAt", t.createdAt() != null ? t.createdAt().toString() : "");
                map.put("completedAt", t.completedAt() != null ? t.completedAt().toString() : "");
                return map;
            })
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("total", content.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workflowId}/force-resolve")
    public ResponseEntity<?> forceResolve(
            @PathVariable String workflowId,
            @Valid @RequestBody ForceResolveRequest request) {
        
        log.info("Force resolving workflow: {}", workflowId);
        
        try {
            WorkflowStub workflowStub = workflowFactory.getWorkflowStub(workflowId);
            
            var signal = new ForceResolveSignal(
                ForceResolveSignal.Action.valueOf(request.action().name()),
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

    private Map<String, Object> mapToResolutionResponse(TransactionResolutionCase case_) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", case_.id());
        response.put("workflowId", case_.workflowId());
        response.put("transactionId", case_.transactionId());
        response.put("proposedAction", case_.proposedAction() != null ? case_.proposedAction().name() : null);
        response.put("reasonCode", case_.reasonCode() != null ? case_.reasonCode() : "");
        response.put("reason", case_.reason() != null ? case_.reason() : "");
        response.put("evidenceUrl", case_.evidenceUrl() != null ? case_.evidenceUrl() : "");
        response.put("status", case_.status().name());
        response.put("makerUserId", case_.makerUserId() != null ? case_.makerUserId() : "");
        response.put("makerCreatedAt", case_.makerCreatedAt() != null ? case_.makerCreatedAt().toString() : "");
        response.put("checkerUserId", case_.checkerUserId() != null ? case_.checkerUserId() : "");
        response.put("checkerAction", case_.checkerAction() != null ? case_.checkerAction() : "");
        response.put("checkerReason", case_.checkerReason() != null ? case_.checkerReason() : "");
        response.put("checkerCompletedAt", case_.checkerCompletedAt() != null ? case_.checkerCompletedAt().toString() : "");
        response.put("makerPendingReason", case_.makerPendingReason() != null ? case_.makerPendingReason() : "");
        response.put("checkerPendingReason", case_.checkerPendingReason() != null ? case_.checkerPendingReason() : "");
        response.put("createdAt", case_.createdAt().toString());
        response.put("updatedAt", case_.updatedAt().toString());

        // Initialize with defaults to ensure they always display
        response.put("amount", BigDecimal.ZERO);
        response.put("referenceNumber", "");
        response.put("customerFee", BigDecimal.ZERO);

        // Include transaction details from TransactionRecord if available
        transactionRecordRepository.findByWorkflowId(case_.workflowId().toString())
            .ifPresent(tx -> {
                if (tx.amount() != null) response.put("amount", tx.amount());
                if (tx.referenceNumber() != null) response.put("referenceNumber", tx.referenceNumber());
                if (tx.customerFee() != null) response.put("customerFee", tx.customerFee());
            });

        return response;
    }

    private Map<String, Object> buildErrorResponse(String code, String message, String actionCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("action_code", actionCode);
        error.put("trace_id", java.util.UUID.randomUUID().toString());
        error.put("timestamp", java.time.Instant.now().toString());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILED");
        response.put("error", error);
        return response;
    }
}
