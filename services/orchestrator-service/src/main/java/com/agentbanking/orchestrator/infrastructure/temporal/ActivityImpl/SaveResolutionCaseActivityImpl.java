package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SaveResolutionCaseActivity;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SaveResolutionCaseActivityImpl implements SaveResolutionCaseActivity {

    private static final Logger log = LoggerFactory.getLogger(SaveResolutionCaseActivityImpl.class);
    private final ResolutionCaseRepository resolutionCaseRepository;

    public SaveResolutionCaseActivityImpl(ResolutionCaseRepository resolutionCaseRepository) {
        this.resolutionCaseRepository = resolutionCaseRepository;
    }

    @Override
    public void saveResolutionCase(Input input) {
        log.info("Creating resolution case for workflow: {}, reasonCode: {}",
            input.workflowId(), input.reasonCode());

        UUID workflowId = UUID.fromString(input.workflowId());
        UUID transactionId = input.transactionId();

        var resolutionCase = TransactionResolutionCase.createPendingMaker(
            workflowId,
            transactionId,
            "AWAITING_REVIEW"
        );

        var updatedCase = resolutionCase.makerPropose(
            null,
            "SYSTEM",
            input.reasonCode(),
            input.reason(),
            null
        );

        resolutionCaseRepository.save(updatedCase);
        log.info("Resolution case created with id: {}", updatedCase.id());
    }
}