package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.OtpRequestedEvent;
import com.agentbanking.auth.domain.model.PasswordResetConfirmedEvent;
import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherKafkaAdapterTest {

    @Mock
    private StreamBridge streamBridge;

    private NotificationPublisherKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationPublisherKafkaAdapter(streamBridge);
    }

    @Test
    void publishUserCreated_sendsToCorrectBinding() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserCreatedEvent event = UserCreatedEvent.create(
            new UserCreatedEvent.UserCreatedData(
                userId,
                "testuser",
                "test@example.com",
                "+60123456789",
                "Test User",
                "AGENT",
                agentId,
                "SMS",
                "temp123"
            )
        );

        adapter.publishUserCreated(event);

        verify(streamBridge).send(eq("userCreated-out-0"), eq(event));
    }

    @Test
    void publishUserCreationFailed_sendsToCorrectBinding() {
        UUID agentId = UUID.randomUUID();
        UserCreationFailedEvent event = UserCreationFailedEvent.create(
            agentId,
            "TEST-001",
            "Auth service unavailable"
        );

        adapter.publishUserCreationFailed(event);

        verify(streamBridge).send(eq("userCreationFailed-out-0"), eq(event));
    }

    @Test
    void publishPasswordResetConfirmed_sendsToCorrectBinding() {
        UUID userId = UUID.randomUUID();
        PasswordResetConfirmedEvent event = PasswordResetConfirmedEvent.create(
            userId,
            "testuser",
            "test@example.com",
            "+60123456789"
        );

        adapter.publishPasswordResetConfirmed(event);

        verify(streamBridge).send(eq("passwordResetConfirmed-out-0"), eq(event));
    }

    @Test
    void publishOtpRequested_sendsToCorrectBinding() {
        UUID userId = UUID.randomUUID();
        OtpRequestedEvent event = new OtpRequestedEvent(
            UUID.randomUUID(),
            "OTP_REQUESTED",
            java.time.Instant.now(),
            new OtpRequestedEvent.OtpRequestedData(
                userId,
                "testuser",
                "test@example.com",
                "+60123456789",
                "123456",
                "SMS"
            )
        );

        adapter.publishOtpRequested(event);

        verify(streamBridge).send(eq("otpRequested-out-0"), eq(event));
    }
}