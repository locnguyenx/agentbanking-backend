package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "switch-adapter", url = "${switch-adapter.url}")
public interface SwitchAdapterFeignClient {

    @PostMapping("/authorize")
    Map<String, Object> authorize(@RequestBody Map<String, Object> request);
}
