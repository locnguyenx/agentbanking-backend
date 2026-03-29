package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.onboarding.domain.port.out.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for audit log viewing
 */
@RestController
@RequestMapping("/internal/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Search audit logs with optional filters
     * GET /internal/audit-logs?entityType=Agent&fromDate=2026-01-01&toDate=2026-12-31&page=0&size=50
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogRecord> auditLogPage = auditLogRepository.searchAuditLogs(entityType, fromDate, toDate, pageable);
        
        List<Map<String, Object>> content = auditLogPage.getContent().stream()
                .map(this::toResponseMap)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", auditLogPage.getTotalElements());
        response.put("totalPages", auditLogPage.getTotalPages());
        response.put("page", auditLogPage.getNumber());
        
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toResponseMap(AuditLogRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("logId", record.auditId());
        map.put("entityType", record.entityType());
        map.put("entityId", record.entityId());
        map.put("action", record.action());
        map.put("performedBy", record.performedBy());
        map.put("changes", record.changes());
        map.put("ipAddress", record.ipAddress());
        map.put("timestamp", record.timestamp());
        return map;
    }
}
