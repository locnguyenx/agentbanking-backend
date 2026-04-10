package com.agentbanking.audit.infrastructure.persistence;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;

    public AuditLogRepositoryAdapter(JpaAuditLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        return jpaRepository.save(new JpaAuditLogEntity(record)).toRecord();
    }

    @Override
    public AuditLogPage findByFilters(
        String serviceName, String action, String performedBy,
        String outcome, LocalDateTime from, LocalDateTime to, int page, int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        var allLogs = jpaRepository.findAll(pageable);
        
        var filtered = allLogs.getContent().stream()
            .filter(e -> serviceName == null || serviceName.isEmpty() || e.getServiceName().equals(serviceName))
            .filter(e -> action == null || action.isEmpty() || e.getAction().equals(action))
            .filter(e -> performedBy == null || performedBy.isEmpty() || e.getPerformedBy().equals(performedBy))
            .filter(e -> outcome == null || outcome.isEmpty() || e.getOutcome().equals(outcome))
            .filter(e -> from == null || !e.getTimestamp().isBefore(from))
            .filter(e -> to == null || !e.getTimestamp().isAfter(to))
            .toList();
        
        int total = filtered.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        
        var pageContent = start < total ? filtered.subList(start, end) : java.util.Collections.emptyList();
        
        return new AuditLogPage(
            pageContent.stream().map(e -> ((JpaAuditLogEntity) e).toRecord()).toList(),
            page,
            size,
            total,
            (int) Math.ceil((double) total / size)
        );
    }
}
