package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.CreateAgentUserUseCase;
import com.agentbanking.auth.domain.port.out.UserRepository;
import com.agentbanking.auth.infrastructure.web.dto.AgentUserStatusResponse;
import com.agentbanking.auth.infrastructure.web.dto.CreateAgentUserRequest;
import com.agentbanking.auth.infrastructure.web.dto.CreateAgentUserRequestFromId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class AgentUserController {

    private final UserRepository userRepository;
    private final CreateAgentUserUseCase createAgentUserUseCase;

    public AgentUserController(UserRepository userRepository, CreateAgentUserUseCase createAgentUserUseCase) {
        this.userRepository = userRepository;
        this.createAgentUserUseCase = createAgentUserUseCase;
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
    public ResponseEntity<UserRecord> createAgentUserById(@PathVariable UUID agentId, @Valid @RequestBody CreateAgentUserRequestFromId request) {
        UserRecord user = createAgentUserUseCase.createAgentUser(
            agentId,
            request.agentCode(),
            request.phone(),
            request.email(),
            request.businessName()
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
