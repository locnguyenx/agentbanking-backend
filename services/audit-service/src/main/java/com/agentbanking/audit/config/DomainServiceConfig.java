package com.agentbanking.audit.config;

import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import com.agentbanking.audit.domain.service.AuditLogQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public AuditLogQueryService auditLogQueryService(AuditLogRepository auditLogRepository) {
        return new AuditLogQueryService(auditLogRepository);
    }
}
