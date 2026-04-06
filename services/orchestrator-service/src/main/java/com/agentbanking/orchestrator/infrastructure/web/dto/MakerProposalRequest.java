package com.agentbanking.orchestrator.infrastructure.web.dto;

import com.agentbanking.orchestrator.domain.model.ResolutionAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MakerProposalRequest(
    @NotNull ResolutionAction action,
    @NotBlank String reasonCode,
    @NotBlank String reason,
    String evidenceUrl
) {}
