package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.switchadapter.application.usecase.AuditLogServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/audit-logs")
public class SwitchAuditController {

    private final AuditLogServiceImpl auditLogService;

    public SwitchAuditController(AuditLogServiceImpl auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
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
}