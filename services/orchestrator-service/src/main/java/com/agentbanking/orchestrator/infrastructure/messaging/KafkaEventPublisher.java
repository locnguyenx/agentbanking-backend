package com.agentbanking.orchestrator.infrastructure.messaging;

import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String TRANSACTION_TOPIC = "transaction-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Publishing transaction completed event: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_TOPIC, event);
    }

    @Override
    public void publishTransactionFailed(TransactionFailedEvent event) {
        log.info("Publishing transaction failed event: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_TOPIC, event);
    }
}
