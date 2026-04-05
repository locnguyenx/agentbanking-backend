package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.PostToCBSActivity;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class PostToCBSActivityImpl implements PostToCBSActivity {

    private final CbsServicePort cbsServicePort;

    public PostToCBSActivityImpl(CbsServicePort cbsServicePort) {
        this.cbsServicePort = cbsServicePort;
    }

    @Override
    public CbsPostResult postToCbs(CbsPostInput input) {
        return cbsServicePort.postToCbs(input);
    }
}
