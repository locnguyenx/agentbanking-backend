package com.agentbanking.orchestrator.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SwitchAdapterClientFallbackFactory implements FallbackFactory<SwitchAdapterClient> {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterClientFallbackFactory.class);

    @Override
    public SwitchAdapterClient create(Throwable cause) {
        log.error("SwitchAdapterClient fallback triggered due to: {}", cause.getMessage(), cause);
        return request -> ResponseEntity.ok(Map.of(
            "status", "FAILED",
            "responseCode", "SWITCH_TIMEOUT",
            "message", "Switch adapter unavailable"
        ));
    }
}
