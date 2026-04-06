package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.in.ProposeResolutionUseCase;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProposeResolutionUseCaseImpl implements ProposeResolutionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProposeResolutionUseCaseImpl.class);

    private final ResolutionService resolutionService;

    public ProposeResolutionUseCaseImpl(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @Override
    public TransactionResolutionCase propose(Command command) {
        log.info("Maker proposing resolution for workflow: {}", command.workflowId());
        
        var result = resolutionService.makerPropose(
            command.workflowId(),
            command.action(),
            command.makerUserId(),
            command.reasonCode(),
            command.reason(),
            command.evidenceUrl()
        );
        
        log.info("Resolution proposed successfully for workflow: {}", command.workflowId());
        return result;
    }
}
