package com.agentbanking.ledger.infrastructure.external;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.port.out.AgentRepository;
import com.agentbanking.ledger.domain.port.out.AgentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Feign client for querying agent data from Onboarding Service
 */
@Component
@org.springframework.context.annotation.Primary
public class AgentRepositoryImpl implements AgentRepository {

    private final OnboardingServiceFeignClient onboardingServiceClient;

    public AgentRepositoryImpl(OnboardingServiceFeignClient onboardingServiceClient) {
        this.onboardingServiceClient = onboardingServiceClient;
    }


    @Override
    public Optional<AgentFloatRecord> findAgentFloat(UUID agentId) {
        // Agent float is stored in Ledger Service itself, this method might be unnecessary
        // but keeping for interface compliance
        return Optional.empty();
    }

    @Override
    public boolean hasPendingTransactions(UUID agentId) {
        try {
            return onboardingServiceClient.hasPendingTransactions(agentId.toString());
        } catch (Exception e) {
            return false;
        }
    }
}
