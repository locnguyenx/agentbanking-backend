package com.agentbanking.ledger.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RulesServiceFeignClientFallback implements FallbackFactory<RulesServiceFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(RulesServiceFeignClientFallback.class);

    @Override
    public RulesServiceFeignClient create(Throwable cause) {
        log.error("Rules Service unavailable: {}", cause.getMessage());
        return request -> Map.of("passed", false, "errorCode", "ERR_SYS_SERVICE_UNAVAILABLE");
    }
}
