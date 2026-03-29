package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.model.AuditLogRecord;
import com.agentbanking.auth.domain.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log endpoints
 */
@RestController
@RequestMapping("/auth/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLogRecord>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        // TODO: Implement pagination and filtering
        List<AuditLogRecord> logs = auditService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/{auditId}")
    public ResponseEntity<AuditLogRecord> getAuditLog(@PathVariable UUID auditId) {
        AuditLogRecord log = auditService.getAuditLogById(auditId);
        return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
    }
}