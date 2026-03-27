package com.agentbanking.onboarding.domain.service;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.agentbanking.onboarding.domain.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public AgentRecord createAgent(CreateAgentCommand command) {
        agentRepository.findByMykadNumber(command.mykadNumber())
            .ifPresent(existing -> {
                throw new IllegalArgumentException(ErrorCodes.ERR_DUPLICATE_AGENT);
            });

        LocalDateTime now = LocalDateTime.now();
        AgentRecord agent = new AgentRecord(
            UUID.randomUUID(),
            command.agentCode(),
            command.businessName(),
            command.tier(),
            AgentStatus.ACTIVE,
            command.merchantGpsLat(),
            command.merchantGpsLng(),
            command.mykadNumber(),
            command.phoneNumber(),
            now,
            now
        );

        return agentRepository.save(agent);
    }

    public AgentRecord updateAgent(UUID agentId, UpdateAgentCommand command) {
        AgentRecord existing = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException(ErrorCodes.ERR_AGENT_NOT_FOUND));

        AgentRecord updated = new AgentRecord(
            existing.agentId(),
            existing.agentCode(),
            command.businessName(),
            command.tier(),
            existing.status(),
            command.merchantGpsLat(),
            command.merchantGpsLng(),
            existing.mykadNumber(),
            command.phoneNumber(),
            existing.createdAt(),
            LocalDateTime.now()
        );

        return agentRepository.save(updated);
    }

    public AgentRecord deactivateAgent(UUID agentId) {
        AgentRecord existing = agentRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException(ErrorCodes.ERR_AGENT_NOT_FOUND));

        if (agentRepository.hasPendingTransactions(agentId)) {
            throw new IllegalArgumentException(ErrorCodes.ERR_AGENT_HAS_PENDING_TRANSACTIONS);
        }

        AgentRecord deactivated = new AgentRecord(
            existing.agentId(),
            existing.agentCode(),
            existing.businessName(),
            existing.tier(),
            AgentStatus.INACTIVE,
            existing.merchantGpsLat(),
            existing.merchantGpsLng(),
            existing.mykadNumber(),
            existing.phoneNumber(),
            existing.createdAt(),
            LocalDateTime.now()
        );

        return agentRepository.save(deactivated);
    }

    public List<AgentRecord> listAgents(int page, int size) {
        return agentRepository.findAll(page, size);
    }

    public Optional<AgentRecord> findById(UUID agentId) {
        return agentRepository.findById(agentId);
    }
}
