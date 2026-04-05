package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsAuthorizationResult;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cbs-service", url = "${cbs-service.url}")
public interface CbsServiceClient {

    @PostMapping("/internal/authorize")
    CbsAuthorizationResult authorizeAtCbs(@RequestBody CbsAuthorizationInput input);

    @PostMapping("/internal/post")
    CbsPostResult postToCbs(@RequestBody CbsPostInput input);
}
