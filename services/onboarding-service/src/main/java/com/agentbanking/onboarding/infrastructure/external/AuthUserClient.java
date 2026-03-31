package com.agentbanking.onboarding.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-user-client", url = "${auth.service.url:http://auth-iam-service:8087}")
public interface AuthUserClient {

    @PostMapping("/internal/users/agent")
    ResponseEntity<?> createAgentUser(@RequestBody CreateAgentUserRequest request);
}
