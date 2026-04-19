package com.agentbanking.ledger.infrastructure.web.dto;

import java.util.UUID;

public record FloatProvisionRequest(
    UUID agentId,
    String agentTier,
    double geofenceLat,
    double geofenceLng,
    String description,
    String referenceNumber,
    String billerCode,
    String targetBin,
    String destinationAccount,
    String ref1,
    String ref2
) {}
