CREATE TABLE kyc_verification (
    verification_id UUID PRIMARY KEY,
    mykad_number VARCHAR(12) NOT NULL,
    full_name VARCHAR(200),
    date_of_birth DATE,
    age INTEGER,
    aml_status VARCHAR(20) NOT NULL,
    biometric_match VARCHAR(20),
    verification_status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500),
    verified_at TIMESTAMP,
    reviewed_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_mykad ON kyc_verification(mykad_number);
CREATE INDEX idx_kyc_status ON kyc_verification(verification_status);
