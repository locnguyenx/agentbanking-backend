package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.AgentTier;
import com.agentbanking.onboarding.domain.model.CreateAgentUserRequest;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AuthUserCreationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentUserCreationTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AuthUserCreationPort authUserCreationPort;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentRepository, authUserCreationPort);
    }

    @Test
    void createAgent_withSuccessfulFeignCall_shouldSetUserCreationStatusToCreated() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT001",
                "Test Business",
                AgentTier.MICRO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720101-01-1234",
                "+60123456789"
        );

        AgentRecord savedAgent = new AgentRecord(
                UUID.randomUUID(),
                command.agentCode(),
                command.businessName(),
                command.tier(),
                AgentStatus.ACTIVE,
                command.merchantGpsLat(),
                command.merchantGpsLng(),
                command.mykadNumber(),
                command.phoneNumber(),
                UserCreationStatus.PENDING,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authUserCreationPort.createAgentUser(any(CreateAgentUserRequest.class)))
                .thenReturn(true);

        AgentRecord result = agentService.createAgent(command);

        assertEquals(UserCreationStatus.CREATED, result.userCreationStatus());
        assertNull(result.userCreationError());
    }

    @Test
    void createAgent_withFailedFeignCall_shouldSetUserCreationStatusToFailed() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT002",
                "Test Business 2",
                AgentTier.STANDARD,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720102-01-1234",
                "+60123456780"
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authUserCreationPort.createAgentUser(any(CreateAgentUserRequest.class)))
                .thenReturn(false);

        AgentRecord result = agentService.createAgent(command);

        assertEquals(UserCreationStatus.FAILED, result.userCreationStatus());
        assertNotNull(result.userCreationError());
    }

    @Test
    void createAgent_withException_shouldSetUserCreationStatusToFailed() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT003",
                "Test Business 3",
                AgentTier.MICRO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720103-01-1234",
                "+60123456781"
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authUserCreationPort.createAgentUser(any(CreateAgentUserRequest.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        AgentRecord result = agentService.createAgent(command);

        assertEquals(UserCreationStatus.FAILED, result.userCreationStatus());
        assertNotNull(result.userCreationError());
    }

    @Test
    void createAgent_shouldCallAuthServiceWithCorrectData() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT004",
                "Test Business 4",
                AgentTier.STANDARD,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720104-01-1234",
                "+60123456782"
        );

        AgentRecord savedAgent = new AgentRecord(
                UUID.randomUUID(),
                command.agentCode(),
                command.businessName(),
                command.tier(),
                AgentStatus.ACTIVE,
                command.merchantGpsLat(),
                command.merchantGpsLng(),
                command.mykadNumber(),
                command.phoneNumber(),
                UserCreationStatus.PENDING,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ArgumentCaptor<CreateAgentUserRequest> requestCaptor = ArgumentCaptor.forClass(CreateAgentUserRequest.class);

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenReturn(savedAgent);
        when(authUserCreationPort.createAgentUser(requestCaptor.capture()))
                .thenReturn(true);

        agentService.createAgent(command);

        CreateAgentUserRequest capturedRequest = requestCaptor.getValue();
        assertEquals(savedAgent.agentId(), capturedRequest.agentId());
        assertEquals(command.agentCode(), capturedRequest.agentCode());
        assertEquals(command.phoneNumber(), capturedRequest.phone());
        assertEquals(command.businessName(), capturedRequest.businessName());
    }

    @Test
    void createAgent_shouldPersistAgentFirstWithPendingStatus() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT005",
                "Test Business 5",
                AgentTier.MICRO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720105-01-1234",
                "+60123456783"
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(authUserCreationPort.createAgentUser(any(CreateAgentUserRequest.class)))
                .thenReturn(true);

        agentService.createAgent(command);

        ArgumentCaptor<AgentRecord> agentCaptor = ArgumentCaptor.forClass(AgentRecord.class);
        verify(agentRepository, times(2)).save(agentCaptor.capture());

        AgentRecord firstSave = agentCaptor.getAllValues().get(0);
        assertEquals(UserCreationStatus.PENDING, firstSave.userCreationStatus());
    }

    @Test
    void createAgent_withDuplicateMykad_shouldThrowException() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT006",
                "Test Business 6",
                AgentTier.STANDARD,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720106-01-1234",
                "+60123456784"
        );

        AgentRecord existingAgent = new AgentRecord(
                UUID.randomUUID(),
                "EXISTING001",
                "Existing Business",
                AgentTier.MICRO,
                AgentStatus.ACTIVE,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                command.mykadNumber(),
                "+60111111111",
                UserCreationStatus.CREATED,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber()))
                .thenReturn(Optional.of(existingAgent));

        assertThrows(Exception.class, () -> agentService.createAgent(command));

        verify(authUserCreationPort, never()).createAgentUser(any());
    }

    @Test
    void createAgent_shouldStoreAgentIdInUserCreation() {
        CreateAgentCommand command = new CreateAgentCommand(
                "AGENT007",
                "Test Business 7",
                AgentTier.MICRO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(101.5),
                "720107-01-1234",
                "+60123456785"
        );

        when(agentRepository.findByMykadNumber(command.mykadNumber())).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authUserCreationPort.createAgentUser(any(CreateAgentUserRequest.class)))
                .thenReturn(true);

        ArgumentCaptor<CreateAgentUserRequest> requestCaptor = ArgumentCaptor.forClass(CreateAgentUserRequest.class);

        agentService.createAgent(command);

        verify(authUserCreationPort, times(1)).createAgentUser(requestCaptor.capture());
        assertNotNull(requestCaptor.getValue().agentId());
        assertEquals(command.agentCode(), requestCaptor.getValue().agentCode());
    }
}
