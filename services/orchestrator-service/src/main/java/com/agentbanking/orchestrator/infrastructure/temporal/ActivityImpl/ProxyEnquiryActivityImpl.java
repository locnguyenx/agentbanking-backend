package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ProxyEnquiryActivity;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.ProxyEnquiryResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class ProxyEnquiryActivityImpl implements ProxyEnquiryActivity {

    private final SwitchAdapterPort switchAdapterPort;

    public ProxyEnquiryActivityImpl(SwitchAdapterPort switchAdapterPort) {
        this.switchAdapterPort = switchAdapterPort;
    }

    @Override
    public ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input) {
        return switchAdapterPort.proxyEnquiry(input);
    }
}
