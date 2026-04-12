package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostInput;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.CbsPostResult;




@ActivityInterface
public interface PostToCBSActivity {
    CbsPostResult postToCbs(CbsPostInput input);
}
