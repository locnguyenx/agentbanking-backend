package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ledger-service", url = "${ledger-service.url}", path = "/internal")
public interface LedgerServiceClient {

    @PostMapping("/debit")
    Map<String, Object> blockFloat(@RequestBody Map<String, Object> request);

    @PostMapping("/credit")
    Map<String, Object> commitFloat(@RequestBody Map<String, Object> request);

    @PostMapping("/reverse")
    Map<String, Object> rollbackFloat(@RequestBody Map<String, Object> request);
}
