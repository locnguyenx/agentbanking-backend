package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.UserRecord;
import java.util.UUID;

public interface CreateAgentUserUseCase {
    UserRecord createAgentUser(UUID agentId, String agentCode, String phone, String email, String businessName);
}
