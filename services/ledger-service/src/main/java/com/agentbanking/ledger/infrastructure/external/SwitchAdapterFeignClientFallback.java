package com.agentbanking.ledger.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SwitchAdapterFeignClientFallback implements SwitchAdapterFeignClient {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterFeignClientFallback.class);

    @Override
    public Map<String, Object> authorize(Map<String, Object> request) {
        log.error("Switch Adapter is unavailable. Fallback returning decline.");
        return Map.of(
            "approved", false,
            "declineCode", "ERR_SWITCH_UNAVAILABLE"
        );
    }
}
