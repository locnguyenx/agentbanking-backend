package com.agentbanking.onboarding.domain.service;

import com.agentbanking.common.exception.AgentException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.AgentTier;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.agentbanking.onboarding.domain.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import com.agentbanking.onboarding.domain.port.out.AgentEventPort;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AuthUserCreationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AuthUserCreationPort authUserCreationPort;

    @Mock
    private AgentEventPort agentEventPort;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentRepository, authUserCreationPort, agentEventPort);
    }

    @Test
    void shouldCreateAgentWithActiveStatus() {
        CreateAgentCommand command = new CreateAgentCommand(
            "AGN001",
            "Test Business",
            AgentTier.STANDARD,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567"
        );

        when(agentRepository.findByMykadNumber("880101011234")).thenReturn(Optional.empty());
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> {
            AgentRecord saved = invocation.getArgument(0);
            return new AgentRecord(
                UUID.randomUUID(),
                saved.agentCode(),
                saved.businessName(),
                saved.tier(),
                AgentStatus.ACTIVE,
                saved.merchantGpsLat(),
                saved.merchantGpsLng(),
                saved.mykadNumber(),
                saved.phoneNumber(),
                UserCreationStatus.CREATED,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
        });

        AgentRecord result = agentService.createAgent(command);

        assertNotNull(result);
        assertEquals(AgentStatus.ACTIVE, result.status());
        assertEquals("AGN001", result.agentCode());
        assertEquals("Test Business", result.businessName());
        assertEquals(AgentTier.STANDARD, result.tier());
    }

    @Test
    void shouldRejectDuplicateAgentCreation() {
        UUID existingId = UUID.randomUUID();
        AgentRecord existing = new AgentRecord(
            existingId,
            "AGN001",
            "Existing Business",
            AgentTier.STANDARD,
            AgentStatus.ACTIVE,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567",
            UserCreationStatus.CREATED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        CreateAgentCommand command = new CreateAgentCommand(
            "AGN002",
            "New Business",
            AgentTier.MICRO,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60198765432"
        );

        when(agentRepository.findByMykadNumber("880101011234")).thenReturn(Optional.of(existing));

        AgentException exception = assertThrows(
            AgentException.class,
            () -> agentService.createAgent(command)
        );

        assertEquals(ErrorCodes.ERR_DUPLICATE_AGENT, exception.getErrorCode());
    }

    @Test
    void shouldRejectDeactivationWithPendingTransactions() {
        UUID agentId = UUID.randomUUID();
        AgentRecord agent = new AgentRecord(
            agentId,
            "AGN001",
            "Test Business",
            AgentTier.STANDARD,
            AgentStatus.ACTIVE,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567",
            UserCreationStatus.CREATED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.hasPendingTransactions(agentId)).thenReturn(true);

        AgentException exception = assertThrows(
            AgentException.class,
            () -> agentService.deactivateAgent(agentId)
        );

        assertEquals(ErrorCodes.ERR_AGENT_HAS_PENDING_TRANSACTIONS, exception.getErrorCode());
    }

    @Test
    void shouldSuccessfullyDeactivateAgent() {
        UUID agentId = UUID.randomUUID();
        AgentRecord agent = new AgentRecord(
            agentId,
            "AGN001",
            "Test Business",
            AgentTier.STANDARD,
            AgentStatus.ACTIVE,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567",
            UserCreationStatus.CREATED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(agentRepository.hasPendingTransactions(agentId)).thenReturn(false);
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentRecord result = agentService.deactivateAgent(agentId);

        assertEquals(AgentStatus.INACTIVE, result.status());
    }

    @Test
    void shouldUpdateAgent() {
        UUID agentId = UUID.randomUUID();
        AgentRecord existing = new AgentRecord(
            agentId,
            "AGN001",
            "Old Business",
            AgentTier.STANDARD,
            AgentStatus.ACTIVE,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567",
            UserCreationStatus.CREATED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        UpdateAgentCommand command = new UpdateAgentCommand(
            "New Business",
            AgentTier.PREMIER,
            BigDecimal.valueOf(4.0000),
            BigDecimal.valueOf(102.0000),
            "+60199999999"
        );

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(existing));
        when(agentRepository.save(any(AgentRecord.class))).thenAnswer(invocation -> {
            AgentRecord saved = invocation.getArgument(0);
            return new AgentRecord(
                agentId,
                saved.agentCode(),
                saved.businessName(),
                saved.tier(),
                saved.status(),
                saved.merchantGpsLat(),
                saved.merchantGpsLng(),
                saved.mykadNumber(),
                saved.phoneNumber(),
                UserCreationStatus.CREATED,
                null,
                existing.createdAt(),
                LocalDateTime.now()
            );
        });

        AgentRecord result = agentService.updateAgent(agentId, command);

        assertEquals("New Business", result.businessName());
        assertEquals(AgentTier.PREMIER, result.tier());
        assertEquals("+60199999999", result.phoneNumber());
    }

    @Test
    void shouldListAgents() {
        List<AgentRecord> agents = List.of(
            new AgentRecord(UUID.randomUUID(), "AGN001", "Business 1", AgentTier.STANDARD, AgentStatus.ACTIVE,
                BigDecimal.valueOf(3.1390), BigDecimal.valueOf(101.6869), "880101011234", "+60191234567",
                UserCreationStatus.CREATED, null, LocalDateTime.now(), LocalDateTime.now()),
            new AgentRecord(UUID.randomUUID(), "AGN002", "Business 2", AgentTier.MICRO, AgentStatus.INACTIVE,
                BigDecimal.valueOf(3.1390), BigDecimal.valueOf(101.6869), "880102011234", "+60191234568",
                UserCreationStatus.CREATED, null, LocalDateTime.now(), LocalDateTime.now())
        );

        when(agentRepository.findAll(0, 20)).thenReturn(agents);

        List<AgentRecord> result = agentService.listAgents(0, 20);

        assertEquals(2, result.size());
    }

    @Test
    void shouldFindAgentById() {
        UUID agentId = UUID.randomUUID();
        AgentRecord agent = new AgentRecord(
            agentId,
            "AGN001",
            "Test Business",
            AgentTier.STANDARD,
            AgentStatus.ACTIVE,
            BigDecimal.valueOf(3.1390),
            BigDecimal.valueOf(101.6869),
            "880101011234",
            "+60191234567",
            UserCreationStatus.CREATED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));

        Optional<AgentRecord> result = agentService.findById(agentId);

        assertTrue(result.isPresent());
        assertEquals(agentId, result.get().agentId());
    }
}
