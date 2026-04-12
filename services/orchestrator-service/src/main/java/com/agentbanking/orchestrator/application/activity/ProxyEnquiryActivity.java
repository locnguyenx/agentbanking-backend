package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryResult;




@ActivityInterface
public interface ProxyEnquiryActivity {
    ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input);
}
