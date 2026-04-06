package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationResult;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryResult;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "switch-adapter-service", url = "${switch-adapter-service.url}", 
             fallbackFactory = SwitchAdapterClientFallbackFactory.class)
public interface SwitchAdapterClient {

    @PostMapping("/internal/auth")
    SwitchAuthorizationResult authorizeTransaction(@RequestBody SwitchAuthorizationInput input);

    @PostMapping("/internal/reversal")
    SwitchReversalResult sendReversal(@RequestBody SwitchReversalInput input);

    @PostMapping("/internal/proxy-enquiry")
    ProxyEnquiryResult proxyEnquiry(@RequestBody ProxyEnquiryInput input);

    @PostMapping("/internal/duitnow-transfer")
    DuitNowTransferResult sendDuitNowTransfer(@RequestBody DuitNowTransferInput input);
}
