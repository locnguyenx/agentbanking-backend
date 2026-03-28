package com.agentbanking.onboarding.domain.port.out;

/**
 * Service for verifying business with SSM (Suruhanjaya Syarikat Malaysia)
 */
public interface SsmService {
    SsmResult verifyBusiness(String mykadNumber);

    record SsmResult(
        String businessName,
        String ownerName,
        boolean isActive
    ) {}
}