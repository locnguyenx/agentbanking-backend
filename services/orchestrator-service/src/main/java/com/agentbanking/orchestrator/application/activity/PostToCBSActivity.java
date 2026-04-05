package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PostToCBSActivity {
    CbsPostResult postToCbs(CbsPostInput input);
}
