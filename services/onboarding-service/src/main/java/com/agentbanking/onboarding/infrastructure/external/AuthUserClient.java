package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.model.CreateAgentUserRequest;
import com.agentbanking.onboarding.domain.port.out.AuthUserCreationPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-user-client", url = "${auth.service.url:http://auth-iam-service:8087}")
public interface AuthUserClient extends AuthUserCreationPort {

    @PostMapping("/internal/users/agent")
    ResponseEntity<?> createAgentUserEndpoint(@RequestBody CreateAgentUserRequest request);

    @Override
    default boolean createAgentUser(CreateAgentUserRequest request) {
        ResponseEntity<?> response = createAgentUserEndpoint(request);
        return response.getStatusCode().is2xxSuccessful();
    }
}
