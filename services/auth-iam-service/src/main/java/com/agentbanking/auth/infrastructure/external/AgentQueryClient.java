package com.agentbanking.auth.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "agent-query-client", url = "${onboarding-service.url:http://onboarding-service:8083}")
public interface AgentQueryClient {

    @GetMapping("/backoffice/agents/{id}")
    ResponseEntity<AgentInfoResponse> getAgent(@PathVariable UUID id);

    record AgentInfoResponse(
        UUID agentId,
        String agentCode,
        String businessName,
        String phoneNumber
    ) {}
}