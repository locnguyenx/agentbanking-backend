package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentUserSyncServiceTest {

    @Mock
    private CreateAgentUserUseCase createAgentUserUseCase;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    private AgentUserSyncService agentUserSyncService;

    @BeforeEach
    void setUp() {
        agentUserSyncService = new AgentUserSyncService(
                createAgentUserUseCase,
                userRepository,
                notificationPublisher
        );
    }

    @Test
    void handleAgentCreated_withNewAgent_shouldCreateUserAndPublishEvent() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT001";
        String phone = "+60123456789";
        String email = "agent@example.com";
        String businessName = "Agent Business";

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, phone, email, businessName)
        );

        UserRecord createdUser = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                email,
                phone,
                "tempPassword",
                businessName,
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                true,
                LocalDateTime.now().plusDays(3),
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(createAgentUserUseCase.createAgentUser(agentId, agentCode, phone, email, businessName))
                .thenReturn(createdUser);

        agentUserSyncService.handleAgentCreated(event);

        verify(createAgentUserUseCase).createAgentUser(agentId, agentCode, phone, email, businessName);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(notificationPublisher).publishUserCreated(eventCaptor.capture());

        UserCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(createdUser.userId(), publishedEvent.data().userId());
        assertEquals(agentCode, publishedEvent.data().username());
        assertEquals(email, publishedEvent.data().email());
        assertEquals("SMS", publishedEvent.data().notificationChannel());
    }

    @Test
    void handleAgentCreated_withExistingUser_shouldSkipCreation() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT001";

        UserRecord existingUser = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                "agent@example.com",
                "+60123456789",
                "hashedPassword",
                "Existing Agent",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                false,
                null,
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, "+60123456789", "agent@example.com", "Agent Business")
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.of(existingUser));

        agentUserSyncService.handleAgentCreated(event);

        verify(createAgentUserUseCase, never()).createAgentUser(any(), any(), any(), any(), any());
        verify(notificationPublisher, never()).publishUserCreated(any());
    }

    @Test
    void handleAgentCreated_withNoPhone_shouldUseEmailChannel() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT002";
        String email = "agent2@example.com";
        String businessName = "Agent Business 2";

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, null, email, businessName)
        );

        UserRecord createdUser = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                email,
                null,
                "tempPassword",
                businessName,
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                true,
                LocalDateTime.now().plusDays(3),
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(createAgentUserUseCase.createAgentUser(agentId, agentCode, null, email, businessName))
                .thenReturn(createdUser);

        agentUserSyncService.handleAgentCreated(event);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(notificationPublisher).publishUserCreated(eventCaptor.capture());

        assertEquals("EMAIL", eventCaptor.getValue().data().notificationChannel());
    }

    @Test
    void handleAgentCreated_whenUserCreationFails_shouldPublishFailureEvent() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT003";
        String phone = "+60123456789";
        String email = "agent3@example.com";
        String businessName = "Agent Business 3";

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, phone, email, businessName)
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(createAgentUserUseCase.createAgentUser(agentId, agentCode, phone, email, businessName))
                .thenThrow(new RuntimeException("Database connection failed"));

        agentUserSyncService.handleAgentCreated(event);

        ArgumentCaptor<UserCreationFailedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreationFailedEvent.class);
        verify(notificationPublisher).publishUserCreationFailed(eventCaptor.capture());

        UserCreationFailedEvent failureEvent = eventCaptor.getValue();
        assertEquals(agentId, failureEvent.data().agentId());
        assertEquals(agentCode, failureEvent.data().agentCode());
        assertEquals("Database connection failed", failureEvent.data().error());
    }

    @Test
    void handleAgentCreated_shouldPublishUserCreatedWithCorrectUserType() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT004";

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, "+60123456789", "agent4@example.com", "Agent Business")
        );

        UserRecord createdUser = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                "agent4@example.com",
                "+60123456789",
                "tempPassword",
                "Agent Business",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                true,
                LocalDateTime.now().plusDays(3),
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(createAgentUserUseCase.createAgentUser(any(), any(), any(), any(), any()))
                .thenReturn(createdUser);

        agentUserSyncService.handleAgentCreated(event);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(notificationPublisher).publishUserCreated(eventCaptor.capture());

        assertEquals("EXTERNAL", eventCaptor.getValue().data().userType());
    }

    @Test
    void handleAgentCreated_shouldNotIncludeTempPasswordInEvent() {
        UUID agentId = UUID.randomUUID();
        String agentCode = "AGENT005";

        AgentCreatedEvent event = new AgentCreatedEvent(
                UUID.randomUUID(),
                "AGENT_CREATED",
                java.time.Instant.now(),
                new AgentCreatedEvent.AgentCreatedData(agentId, agentCode, "+60123456789", "agent5@example.com", "Agent Business")
        );

        UserRecord createdUser = new UserRecord(
                UUID.randomUUID(),
                agentCode,
                "agent5@example.com",
                "+60123456789",
                "TempSecret123",
                "Agent Business",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                agentCode,
                true,
                LocalDateTime.now().plusDays(3),
                Set.of("AGENT"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findByAgentId(agentId)).thenReturn(Optional.empty());
        when(createAgentUserUseCase.createAgentUser(any(), any(), any(), any(), any()))
                .thenReturn(createdUser);

        agentUserSyncService.handleAgentCreated(event);

        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(notificationPublisher).publishUserCreated(eventCaptor.capture());

        assertNull(eventCaptor.getValue().data().temporaryPassword());
    }
}
