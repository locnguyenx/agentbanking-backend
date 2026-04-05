package com.agentbanking.audit.infrastructure.persistence;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

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
        var springPage = jpaRepository.findByFilters(
            serviceName, action, performedBy, outcome, from, to,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return new AuditLogPage(
            springPage.getContent().stream().map(JpaAuditLogEntity::toRecord).toList(),
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }
}
