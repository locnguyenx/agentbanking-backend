package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.AmlStatus;

/**
 * Port for AML screening service
 */
public interface AmlScreeningPort {
    AmlStatus screen(String mykadNumber, String fullName);
}