package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.in.ApproveResolutionUseCase;
import com.agentbanking.orchestrator.domain.service.ResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApproveResolutionUseCaseImpl implements ApproveResolutionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApproveResolutionUseCaseImpl.class);

    private final ResolutionService resolutionService;

    public ApproveResolutionUseCaseImpl(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @Override
    public TransactionResolutionCase approve(Command command) {
        log.info("Checker approving resolution for workflow: {}", command.workflowId());
        
        var result = resolutionService.checkerApprove(
            command.workflowId(),
            command.checkerUserId(),
            command.reason()
        );
        
        log.info("Resolution approved successfully for workflow: {}", command.workflowId());
        return result;
    }
}
