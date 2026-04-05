package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ProxyEnquiryActivity {
    ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input);
}
