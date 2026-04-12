package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;




@ActivityInterface
public interface PublishKafkaEventActivity {
    void publishCompleted(TransactionCompletedEvent event);
}
