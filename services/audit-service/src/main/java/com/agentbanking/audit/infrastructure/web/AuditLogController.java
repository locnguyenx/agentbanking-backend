package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditLogController {

    private final QueryAuditLogsUseCase queryAuditLogsUseCase;

    public AuditLogController(QueryAuditLogsUseCase queryAuditLogsUseCase) {
        this.queryAuditLogsUseCase = queryAuditLogsUseCase;
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String performedBy,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        AuditLogPage result = queryAuditLogsUseCase.queryAuditLogs(
            serviceName, action, performedBy, outcome, from, to, page, size
        );
        return ResponseEntity.ok(Map.of(
            "content", result.content(),
            "page", result.page(),
            "size", result.size(),
            "totalElements", result.totalElements(),
            "totalPages", result.totalPages()
        ));
    }

    @GetMapping("/logs/export")
    public void exportAuditLogs(
        @RequestParam(required = false) String serviceName,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String performedBy,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        jakarta.servlet.http.HttpServletResponse response
    ) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");

        var result = queryAuditLogsUseCase.queryAuditLogs(
            serviceName, action, performedBy, outcome, from, to, 0, 10000
        );

        var writer = response.getWriter();
        writer.println("Timestamp,UserID,Action,Resource,IPAddress,Service,Result,FailureReason");
        for (var record : result.content()) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                record.timestamp(), record.performedBy(), record.action(),
                record.entityType(), record.ipAddress() != null ? record.ipAddress() : "",
                record.serviceName(), record.outcome(),
                record.failureReason() != null ? record.failureReason() : "");
        }
        writer.flush();
    }
}
