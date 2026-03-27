package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "switch-adapter-service", url = "${switch-adapter-service.url}", 
             fallbackFactory = SwitchAdapterClientFallbackFactory.class)
public interface SwitchAdapterClient {

    @PostMapping("/internal/authorize")
    ResponseEntity<Map<String, Object>> authorizeTransaction(@RequestBody Map<String, Object> request);
}
