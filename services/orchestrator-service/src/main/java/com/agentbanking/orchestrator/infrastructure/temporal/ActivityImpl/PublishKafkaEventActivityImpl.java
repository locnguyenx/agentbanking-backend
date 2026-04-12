package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.PublishKafkaEventActivity;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class PublishKafkaEventActivityImpl implements PublishKafkaEventActivity {

    private final EventPublisherPort eventPublisherPort;

    public PublishKafkaEventActivityImpl(EventPublisherPort eventPublisherPort) {
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public void publishCompleted(TransactionCompletedEvent event) {
        eventPublisherPort.publishTransactionCompleted(event);
    }
}
