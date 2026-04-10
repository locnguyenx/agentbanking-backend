package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuditLogServiceImpl;
import com.agentbanking.common.audit.AuditLogRecord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/audit")
public class AuditController {
    
    private final AuditLogServiceImpl auditLogService;
    
    public AuditController(AuditLogServiceImpl auditLogService) {
        this.auditLogService = auditLogService;
    }
    
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<AuditLogRecord> logs = auditLogService.getAuditLogs(page, size);
        long total = auditLogService.getTotalCount();
        
        return ResponseEntity.ok(Map.of(
            "content", logs,
            "totalElements", total,
            "totalPages", (total + size - 1) / size,
            "page", page,
            "size", size
        ));
    }
    
    @GetMapping("/logs/{auditId}")
    public ResponseEntity<AuditLogRecord> getAuditLog(@PathVariable UUID auditId) {
        List<AuditLogRecord> logs = auditLogService.getAuditLogs(0, 100);
        return logs.stream()
            .filter(log -> log.auditId().equals(auditId))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}