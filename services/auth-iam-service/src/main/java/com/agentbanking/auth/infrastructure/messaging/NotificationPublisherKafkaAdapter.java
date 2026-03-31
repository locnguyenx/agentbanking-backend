package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationPublisherKafkaAdapter implements NotificationPublisher {

    private final StreamBridge streamBridge;

    public NotificationPublisherKafkaAdapter(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        streamBridge.send("userCreated-out-0", event);
    }

    @Override
    public void publishUserCreationFailed(UserCreationFailedEvent event) {
        streamBridge.send("userCreationFailed-out-0", event);
    }

    @Override
    public void publishPasswordResetConfirmed(PasswordResetConfirmedEvent event) {
        streamBridge.send("passwordResetConfirmed-out-0", event);
    }

    @Override
    public void publishOtpRequested(OtpRequestedEvent event) {
        streamBridge.send("otpRequested-out-0", event);
    }
}
