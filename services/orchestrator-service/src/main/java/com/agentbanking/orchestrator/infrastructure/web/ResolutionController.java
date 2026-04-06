package com.agentbanking.orchestrator.infrastructure.web;

import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.in.ApproveResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.ProposeResolutionUseCase;
import com.agentbanking.orchestrator.domain.port.in.RejectResolutionUseCase;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import com.agentbanking.orchestrator.infrastructure.web.dto.CheckerActionRequest;
import com.agentbanking.orchestrator.infrastructure.web.dto.MakerProposalRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public ResolutionController(ProposeResolutionUseCase proposeResolutionUseCase,
                                 ApproveResolutionUseCase approveResolutionUseCase,
                                 RejectResolutionUseCase rejectResolutionUseCase,
                                 ResolutionService resolutionService) {
        this.proposeResolutionUseCase = proposeResolutionUseCase;
        this.approveResolutionUseCase = approveResolutionUseCase;
        this.rejectResolutionUseCase = rejectResolutionUseCase;
        this.resolutionService = resolutionService;
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

    @GetMapping
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
        response.put("createdAt", case_.createdAt().toString());
        response.put("updatedAt", case_.updatedAt().toString());
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
