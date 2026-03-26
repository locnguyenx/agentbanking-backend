package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.messaging.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionEvent event) {
        log.info("Publishing transaction event: txId={}, status={}",
                event.transactionId(), event.status());
        kafkaTemplate.send(
            KafkaTopics.LEDGER_TRANSACTIONS,
            event.agentId().toString(),
            event
        );
    }
}
