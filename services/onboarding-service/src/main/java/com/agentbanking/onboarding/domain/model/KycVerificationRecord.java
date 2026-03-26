package com.agentbanking.onboarding.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record KycVerificationRecord(
    UUID verificationId,
    String mykadNumber,
    String fullName,
    LocalDate dateOfBirth,
    Integer age,
    AmlStatus amlStatus,
    BiometricResult biometricMatch,
    KycStatus verificationStatus,
    String rejectionReason,
    LocalDateTime verifiedAt,
    String reviewedBy,
    LocalDateTime createdAt
) {}