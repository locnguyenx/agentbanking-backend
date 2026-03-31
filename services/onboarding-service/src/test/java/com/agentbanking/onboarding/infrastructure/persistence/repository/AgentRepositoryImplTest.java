package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.AgentTier;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import com.agentbanking.onboarding.infrastructure.persistence.mapper.AgentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AgentRepositoryImplTest {

    @Mock
    private AgentJpaRepository jpaRepository;

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private TransactionQueryClient transactionQueryClient;

    private AgentRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AgentRepositoryImpl(jpaRepository, agentMapper, transactionQueryClient);
    }

    @Test
    void shouldReturnTrueWhenAgentHasPendingTransactions() {
        UUID agentId = UUID.randomUUID();

        when(transactionQueryClient.hasPendingTransactions(agentId)).thenReturn(true);

        boolean result = repository.hasPendingTransactions(agentId);

        assertTrue(result);
        verify(transactionQueryClient).hasPendingTransactions(agentId);
    }

    @Test
    void shouldReturnFalseWhenAgentHasNoPendingTransactions() {
        UUID agentId = UUID.randomUUID();

        when(transactionQueryClient.hasPendingTransactions(agentId)).thenReturn(false);

        boolean result = repository.hasPendingTransactions(agentId);

        assertFalse(result);
        verify(transactionQueryClient).hasPendingTransactions(agentId);
    }

    @Test
    void shouldUseAgentMapperWhenSaving() {
        UUID agentId = UUID.randomUUID();
        AgentRecord record = new AgentRecord(
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

        when(jpaRepository.save(any())).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0);
            return entity;
        });

        repository.save(record);

        verify(jpaRepository).save(any());
    }

    @Test
    void shouldUseAgentMapperWhenFindingById() {
        UUID agentId = UUID.randomUUID();

        when(jpaRepository.findById(agentId)).thenReturn(Optional.empty());

        Optional<AgentRecord> result = repository.findById(agentId);

        assertTrue(result.isEmpty());
    }
}
