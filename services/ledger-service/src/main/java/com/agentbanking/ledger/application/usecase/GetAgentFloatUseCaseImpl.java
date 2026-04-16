package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.port.in.GetAgentFloatUseCase;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetAgentFloatUseCaseImpl implements GetAgentFloatUseCase {

    private final AgentFloatRepository agentFloatRepository;

    public GetAgentFloatUseCaseImpl(AgentFloatRepository agentFloatRepository) {
        this.agentFloatRepository = agentFloatRepository;
    }

    @Override
    public AgentFloatRecord getAgentFloat(UUID agentId) {
        return agentFloatRepository.findById(agentId);
    }

    @Override
    public boolean exists(UUID agentId) {
        return agentFloatRepository.findById(agentId) != null;
    }
}