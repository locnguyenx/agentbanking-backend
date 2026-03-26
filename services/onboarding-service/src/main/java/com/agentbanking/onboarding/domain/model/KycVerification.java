package com.agentbanking.onboarding.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class KycVerification {
    private UUID verificationId;
    private String mykadNumber;
    private String fullName;
    private LocalDate dateOfBirth;
    private Integer age;
    private AmlStatus amlStatus;
    private BiometricResult biometricMatch;
    private KycStatus verificationStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private String reviewedBy;
    private LocalDateTime createdAt;

    public KycVerification() {}

    public KycVerificationRecord toRecord() {
        return new KycVerificationRecord(
            verificationId, mykadNumber, fullName, dateOfBirth, age,
            amlStatus, biometricMatch, verificationStatus, rejectionReason,
            verifiedAt, reviewedBy, createdAt
        );
    }

    public static KycVerification fromRecord(KycVerificationRecord record) {
        KycVerification v = new KycVerification();
        v.verificationId = record.verificationId();
        v.mykadNumber = record.mykadNumber();
        v.fullName = record.fullName();
        v.dateOfBirth = record.dateOfBirth();
        v.age = record.age();
        v.amlStatus = record.amlStatus();
        v.biometricMatch = record.biometricMatch();
        v.verificationStatus = record.verificationStatus();
        v.rejectionReason = record.rejectionReason();
        v.verifiedAt = record.verifiedAt();
        v.reviewedBy = record.reviewedBy();
        v.createdAt = record.createdAt();
        return v;
    }

    public UUID getVerificationId() { return verificationId; }
    public void setVerificationId(UUID verificationId) { this.verificationId = verificationId; }
    public String getMykadNumber() { return mykadNumber; }
    public void setMykadNumber(String mykadNumber) { this.mykadNumber = mykadNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public AmlStatus getAmlStatus() { return amlStatus; }
    public void setAmlStatus(AmlStatus amlStatus) { this.amlStatus = amlStatus; }
    public BiometricResult getBiometricMatch() { return biometricMatch; }
    public void setBiometricMatch(BiometricResult biometricMatch) { this.biometricMatch = biometricMatch; }
    public KycStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(KycStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}