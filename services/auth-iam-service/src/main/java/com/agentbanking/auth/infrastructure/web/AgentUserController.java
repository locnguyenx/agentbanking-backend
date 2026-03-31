package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.port.out.UserRepository;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.infrastructure.web.dto.AgentUserStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
public class AgentUserController {

    private final UserRepository userRepository;

    public AgentUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
