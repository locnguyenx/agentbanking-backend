package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreatedEvent;
import com.agentbanking.auth.domain.model.UserCreationFailedEvent;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AgentUserSyncService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentUserSyncService.class);
    
    private final CreateAgentUserUseCase createAgentUserUseCase;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;

    public AgentUserSyncService(CreateAgentUserUseCase createAgentUserUseCase,
                                UserRepository userRepository,
                                NotificationPublisher notificationPublisher) {
        this.createAgentUserUseCase = createAgentUserUseCase;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public void handleAgentCreated(AgentCreatedEvent event) {
        var data = event.data();
        
        Optional<?> existingUser = userRepository.findByAgentId(data.agentId());
        if (existingUser.isPresent()) {
            log.info("User already exists for agent {}, skipping", data.agentCode());
            return;
        }
        
        try {
            var user = createAgentUserUseCase.createAgentUser(
                data.agentId(),
                data.agentCode(),
                data.phoneNumber(),
                data.email(),
                data.businessName()
            );
            
            notificationPublisher.publishUserCreated(
                UserCreatedEvent.create(new UserCreatedEvent.UserCreatedData(
                    user.userId(),
                    user.username(),
                    user.email(),
                    user.phone(),
                    user.fullName(),
                    user.userType().name(),
                    user.agentId(),
                    data.phoneNumber() != null ? "SMS" : "EMAIL",
                    null
                ))
            );
        } catch (Exception e) {
            log.error("Failed to create user for agent {}: {}", data.agentCode(), e.getMessage());
            notificationPublisher.publishUserCreationFailed(
                UserCreationFailedEvent.create(data.agentId(), data.agentCode(), e.getMessage())
            );
        }
    }
}
