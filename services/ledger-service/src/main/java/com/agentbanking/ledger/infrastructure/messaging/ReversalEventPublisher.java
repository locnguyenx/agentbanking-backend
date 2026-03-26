package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.messaging.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReversalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReversalEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReversalEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ReversalEvent event) {
        log.info("Publishing reversal event: originalTxId={}, reversalTxId={}, reason={}",
                event.originalTransactionId(), event.reversalTransactionId(), event.reason());
        kafkaTemplate.send(
            KafkaTopics.LEDGER_REVERSALS,
            event.agentId().toString(),
            event
        );
    }
}
