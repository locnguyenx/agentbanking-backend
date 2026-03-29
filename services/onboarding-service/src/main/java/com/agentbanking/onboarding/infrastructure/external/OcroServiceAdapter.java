package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.port.out.OcroService;
import org.springframework.stereotype.Component;

@Component
public class OcroServiceAdapter implements OcroService {

    @Override
    public String extractNameFromMyKad(String mykadNumber) {
        return "Extracted Name";
    }
}
