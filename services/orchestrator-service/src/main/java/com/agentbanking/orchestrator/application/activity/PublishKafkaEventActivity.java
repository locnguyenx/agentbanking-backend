package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PublishKafkaEventActivity {
    void publishCompleted(TransactionCompletedEvent event);
}
