package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.port.out.SsmService;
import org.springframework.stereotype.Component;

@Component
public class SsmServiceAdapter implements SsmService {

    @Override
    public SsmResult verifyBusiness(String mykadNumber) {
        return new SsmResult("Sample Business", "Owner Name", true);
    }
}
