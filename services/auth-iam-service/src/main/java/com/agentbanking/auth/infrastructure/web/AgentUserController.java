package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.UserRepository;
import com.agentbanking.auth.infrastructure.external.AgentQueryClient;
import com.agentbanking.auth.infrastructure.web.dto.AgentUserStatusResponse;
import com.agentbanking.auth.infrastructure.web.dto.CreateAgentUserRequest;
import com.agentbanking.auth.infrastructure.web.dto.CreateAgentUserRequestFromId;
import com.agentbanking.auth.infrastructure.web.dto.ErrorResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class AgentUserController {

    private static final Logger log = LoggerFactory.getLogger(AgentUserController.class);

    private final UserRepository userRepository;
    private final CreateAgentUserUseCase createAgentUserUseCase;
    private final AgentQueryClient agentQueryClient;

    public AgentUserController(UserRepository userRepository, 
                           CreateAgentUserUseCase createAgentUserUseCase,
                           AgentQueryClient agentQueryClient) {
        this.userRepository = userRepository;
        this.createAgentUserUseCase = createAgentUserUseCase;
        this.agentQueryClient = agentQueryClient;
    }

    @PostMapping("/agent")
    public ResponseEntity<UserRecord> createAgentUser(@Valid @RequestBody CreateAgentUserRequest request) {
        UserRecord user = createAgentUserUseCase.createAgentUser(
            request.agentId(),
            request.agentCode(),
            request.phone(),
            request.email(),
            request.businessName()
        );
        return ResponseEntity.ok(user);
    }

    @PostMapping("/agent/{agentId}/create")
    public ResponseEntity<?> createAgentUserById(@PathVariable UUID agentId, @RequestBody CreateAgentUserRequestFromId request) {
        var existingUser = userRepository.findByAgentId(agentId);
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(
                new ErrorResponseDto("ERR_USER_ALREADY_EXISTS", "User account already exists for this agent", "DECLINE")
            );
        }

        String agentCode = request.agentCode();
        String phone = request.phone();
        String email = request.email();
        String businessName = request.businessName();

        try {
            var agentResponse = agentQueryClient.getAgent(agentId);
            if (agentResponse.getStatusCode().is2xxSuccessful() && agentResponse.getBody() != null) {
                var agent = agentResponse.getBody();
                if (agentCode == null || agentCode.isBlank()) {
                    agentCode = agent.agentCode();
                }
                if (phone == null || phone.isBlank()) {
                    phone = agent.phoneNumber();
                }
                if (businessName == null || businessName.isBlank()) {
                    businessName = agent.businessName();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query agent from onboarding service: {}", e.getMessage());
        }

        if (agentCode == null || agentCode.isBlank()) {
            return ResponseEntity.badRequest().body(
                new ErrorResponseDto("ERR_VAL_INVALID_REQUEST", "Agent code is required", "DECLINE")
            );
        }

        UserRecord user = createAgentUserUseCase.createAgentUser(
            agentId,
            agentCode,
            phone,
            email,
            businessName
        );
        return ResponseEntity.ok(user);
    }

    @GetMapping("/agent/{agentId}/status")
    public ResponseEntity<AgentUserStatusResponse> getAgentUserStatus(@PathVariable UUID agentId) {
        return userRepository.findByAgentId(agentId)
            .map(user -> ResponseEntity.ok(new AgentUserStatusResponse(
                agentId,
                user.status().name(),
                user.userId(),
                null
            )))
            .orElse(ResponseEntity.ok(new AgentUserStatusResponse(
                agentId,
                "NOT_CREATED",
                null,
                null
            )));
    }
}
