package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.OtpRequestedEvent;

public interface NotificationPublisher {
    void publishUserCreated(UserCreatedEvent event);
    void publishUserCreationFailed(UserCreationFailedEvent event);
    void publishPasswordResetConfirmed(PasswordResetConfirmedEvent event);
    void publishOtpRequested(OtpRequestedEvent event);
}
