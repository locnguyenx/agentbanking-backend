package com.agentbanking.onboarding.domain.service;

import com.agentbanking.common.exception.AgentException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.model.UserCreationStatus;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase.CreateAgentCommand;
import com.agentbanking.onboarding.domain.port.in.UpdateAgentUseCase.UpdateAgentCommand;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.infrastructure.external.AuthUserClient;
import com.agentbanking.onboarding.infrastructure.external.CreateAgentUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final AuthUserClient authUserClient;

    public AgentService(AgentRepository agentRepository, AuthUserClient authUserClient) {
        this.agentRepository = agentRepository;
        this.authUserClient = authUserClient;
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
            
            ResponseEntity<?> response = authUserClient.createAgentUser(request);
            
            if (response.getStatusCode().is2xxSuccessful()) {
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
                log.info("Agent user created successfully in auth-iam-service for agentId: {}", savedAgent.agentId());
                return agentRepository.save(successAgent);
            } else {
                log.warn("Failed to create agent user in auth-iam-service, status: {}", response.getStatusCode());
                return updateUserCreationStatus(savedAgent, UserCreationStatus.FAILED, "Auth service returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error creating agent user in auth-iam-service for agentId: {}, error: {}", savedAgent.agentId(), e.getMessage());
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
}
