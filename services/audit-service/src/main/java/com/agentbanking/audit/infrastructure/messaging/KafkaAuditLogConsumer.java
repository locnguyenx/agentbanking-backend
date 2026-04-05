package com.agentbanking.audit.infrastructure.messaging;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class KafkaAuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditLogConsumer.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KafkaAuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Bean
    public Consumer<JsonNode> auditLogIn() {
        return event -> {
            try {
                JsonNode entityIdNode = event.get("entityId");
                UUID entityId = (entityIdNode != null && !entityIdNode.isNull())
                    ? UUID.fromString(entityIdNode.asText())
                    : null;

                String changes = null;
                if (event.has("changes") && !event.get("changes").isNull()) {
                    changes = objectMapper.writeValueAsString(event.get("changes"));
                }

                AuditLogRecord record = new AuditLogRecord(
                    UUID.fromString(event.get("auditId").asText()),
                    event.get("serviceName").asText(),
                    event.get("entityType").asText(),
                    entityId,
                    event.get("action").asText(),
                    event.get("performedBy").asText(),
                    event.has("ipAddress") && !event.get("ipAddress").isNull() ? event.get("ipAddress").asText() : null,
                    LocalDateTime.parse(event.get("timestamp").asText()),
                    event.get("outcome").asText(),
                    event.has("failureReason") && !event.get("failureReason").isNull() ? event.get("failureReason").asText() : null,
                    changes,
                    LocalDateTime.now()
                );
                auditLogRepository.save(record);
                log.debug("Stored audit event: {} for {} {}", record.action(), record.entityType(), record.entityId());
            } catch (Exception e) {
                log.error("Failed to process audit event: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
