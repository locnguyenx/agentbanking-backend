package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.port.in.CreateAgentFloatUseCase;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class CreateAgentFloatUseCaseImpl implements CreateAgentFloatUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateAgentFloatUseCaseImpl.class);
    private final AgentFloatRepository agentFloatRepository;

    public CreateAgentFloatUseCaseImpl(AgentFloatRepository agentFloatRepository) {
        this.agentFloatRepository = agentFloatRepository;
    }

    @Override
    public AgentFloatRecord createAgentFloat(UUID agentId, BigDecimal initialBalance, String currency) {
        log.info("Creating agent float for agentId: {}, initialBalance: {}, currency: {}", 
                agentId, initialBalance, currency);

        AgentFloatRecord existing = agentFloatRepository.findById(agentId);
        if (existing != null) {
            throw new IllegalStateException("Agent float already exists for agent: " + agentId);
        }

        AgentFloatRecord newFloat = new AgentFloatRecord(
                UUID.randomUUID(),
                agentId,
                initialBalance.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO,
                currency.toUpperCase(),
                0L,
                null,
                null
        );

        return agentFloatRepository.save(newFloat);
    }
}