package com.agentbanking.onboarding.domain.service;

import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.model.BiometricResult;
import com.agentbanking.onboarding.domain.model.KycStatus;
import org.springframework.stereotype.Service;

@Service
public class KycDecisionService {

    /**
     * Determines KYC verification status based on:
     * - biometricMatch: MATCH or NO_MATCH
     * - amlStatus: CLEAN, FLAGGED, or BLOCKED
     * - age: calculated from DOB
     * 
     * Decision rules (BDD-O03):
     * - age < 18 → REJECTED
     * - amlStatus = BLOCKED → REJECTED
     * - amlStatus = FLAGGED → MANUAL_REVIEW
     * - biometricMatch = NO_MATCH → MANUAL_REVIEW
     * - All conditions pass → AUTO_APPROVED
     */
    public KycStatus decide(BiometricResult biometricMatch, AmlStatus amlStatus, int age) {
        // Rule 1: age < 18 → REJECT
        if (age < 18) {
            return KycStatus.REJECTED;
        }
        
        // Rule 2: amlStatus = BLOCKED → REJECT
        if (amlStatus == AmlStatus.BLOCKED) {
            return KycStatus.REJECTED;
        }
        
        // Rule 3: amlStatus = FLAGGED → MANUAL_REVIEW
        if (amlStatus == AmlStatus.FLAGGED) {
            return KycStatus.MANUAL_REVIEW;
        }
        
        // Rule 4: biometricMatch = NO_MATCH → MANUAL_REVIEW
        if (biometricMatch == BiometricResult.NO_MATCH) {
            return KycStatus.MANUAL_REVIEW;
        }
        
        // All conditions pass → AUTO_APPROVED
        return KycStatus.AUTO_APPROVED;
    }
}
