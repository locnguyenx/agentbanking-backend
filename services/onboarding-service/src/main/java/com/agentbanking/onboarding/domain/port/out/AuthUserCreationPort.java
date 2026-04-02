package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.CreateAgentUserRequest;

public interface AuthUserCreationPort {
    boolean createAgentUser(CreateAgentUserRequest request);
}
