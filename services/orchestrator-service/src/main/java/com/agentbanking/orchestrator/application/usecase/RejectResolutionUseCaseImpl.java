package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.in.RejectResolutionUseCase;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RejectResolutionUseCaseImpl implements RejectResolutionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RejectResolutionUseCaseImpl.class);

    private final ResolutionService resolutionService;

    public RejectResolutionUseCaseImpl(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @Override
    public TransactionResolutionCase reject(Command command) {
        log.info("Checker rejecting resolution for workflow: {}", command.workflowId());
        
        var result = resolutionService.checkerReject(
            command.workflowId(),
            command.checkerUserId(),
            command.reason()
        );
        
        log.info("Resolution rejected successfully for workflow: {}", command.workflowId());
        return result;
    }
}
