package com.agentbanking.onboarding.domain.service;

import com.agentbanking.common.event.AgentCreatedEvent;
import com.agentbanking.common.exception.AgentException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.CreateAgentUserRequest;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.agentbanking.onboarding.domain.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import com.agentbanking.onboarding.domain.port.out.AgentEventPort;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.domain.port.out.AuthUserCreationPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AgentService {

    private final AgentRepository agentRepository;
    private final AuthUserCreationPort authUserCreationPort;
    private final AgentEventPort agentEventPort;

    public AgentService(AgentRepository agentRepository, AuthUserCreationPort authUserCreationPort, AgentEventPort agentEventPort) {
        this.agentRepository = agentRepository;
        this.authUserCreationPort = authUserCreationPort;
        this.agentEventPort = agentEventPort;
    }

    public AgentRecord createAgent(CreateAgentCommand command) {
        agentRepository.findByMykadNumber(command.mykadNumber())
            .ifPresent(existing -> {
                throw new AgentException(ErrorCodes.ERR_DUPLICATE_AGENT, "Agent with this MyKad number already exists");
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
            UserCreationStatus.PENDING,
            null,
            now,
            now
        );

        AgentRecord savedAgent = agentRepository.save(agent);

        try {
            CreateAgentUserRequest request = new CreateAgentUserRequest(
                savedAgent.agentId(),
                savedAgent.agentCode(),
                savedAgent.phoneNumber(),
                null,
                savedAgent.businessName()
            );

            boolean success = authUserCreationPort.createAgentUser(request);

            if (success) {
                AgentRecord successAgent = new AgentRecord(
                    savedAgent.agentId(),
                    savedAgent.agentCode(),
                    savedAgent.businessName(),
                    savedAgent.tier(),
                    savedAgent.status(),
                    savedAgent.merchantGpsLat(),
                    savedAgent.merchantGpsLng(),
                    savedAgent.mykadNumber(),
                    savedAgent.phoneNumber(),
                    UserCreationStatus.CREATED,
                    null,
                    savedAgent.createdAt(),
                    LocalDateTime.now()
                );
                AgentRecord finalAgent = agentRepository.save(successAgent);
                publishAgentCreatedEvent(finalAgent);
                return finalAgent;
            } else {
                return updateUserCreationStatus(savedAgent, UserCreationStatus.FAILED, "Auth service returned non-success status");
            }
        } catch (Exception e) {
            return updateUserCreationStatus(savedAgent, UserCreationStatus.FAILED, e.getMessage());
        }
    }

    private AgentRecord updateUserCreationStatus(AgentRecord agent, UserCreationStatus status, String error) {
        AgentRecord updated = new AgentRecord(
            agent.agentId(),
            agent.agentCode(),
            agent.businessName(),
            agent.tier(),
            agent.status(),
            agent.merchantGpsLat(),
            agent.merchantGpsLng(),
            agent.mykadNumber(),
            agent.phoneNumber(),
            status,
            error,
            agent.createdAt(),
            LocalDateTime.now()
        );
        return agentRepository.save(updated);
    }

    public AgentRecord updateAgent(UUID agentId, UpdateAgentCommand command) {
        AgentRecord existing = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentException(ErrorCodes.ERR_AGENT_NOT_FOUND, "Agent not found"));

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
            existing.userCreationStatus(),
            existing.userCreationError(),
            existing.createdAt(),
            LocalDateTime.now()
        );

        return agentRepository.save(updated);
    }

    public AgentRecord deactivateAgent(UUID agentId) {
        AgentRecord existing = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentException(ErrorCodes.ERR_AGENT_NOT_FOUND, "Agent not found"));

        if (agentRepository.hasPendingTransactions(agentId)) {
            throw new AgentException(ErrorCodes.ERR_AGENT_HAS_PENDING_TRANSACTIONS, "Agent has pending transactions");
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
            existing.userCreationStatus(),
            existing.userCreationError(),
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

    private void publishAgentCreatedEvent(AgentRecord agent) {
        try {
            AgentCreatedEvent event = AgentCreatedEvent.create(
                agent.agentId(),
                agent.agentCode(),
                agent.phoneNumber(),
                null,
                agent.businessName(),
                agent.tier().name(),
                agent.merchantGpsLat() != null ? agent.merchantGpsLat().doubleValue() : null,
                agent.merchantGpsLng() != null ? agent.merchantGpsLng().doubleValue() : null
            );
            agentEventPort.publishAgentCreated(event);
        } catch (Exception e) {
        }
    }
}
