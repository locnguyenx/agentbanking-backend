CREATE TABLE agent_onboarding (
    onboarding_id UUID PRIMARY KEY,
    mykad_number VARCHAR(12) NOT NULL,
    extracted_name VARCHAR(200),
    ssm_business_name VARCHAR(200),
    ssm_owner_name VARCHAR(200),
    agent_tier VARCHAR(20) NOT NULL,
    ocr_name_match BOOLEAN NOT NULL DEFAULT FALSE,
    ssm_active BOOLEAN NOT NULL DEFAULT FALSE,
    ssm_owner_match BOOLEAN NOT NULL DEFAULT FALSE,
    aml_clean BOOLEAN NOT NULL DEFAULT FALSE,
    gps_low_risk BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE onboarding_decision (
    decision_id UUID PRIMARY KEY,
    onboarding_id UUID NOT NULL,
    decision_type VARCHAR(20) NOT NULL,
    reason TEXT,
    reviewer_id VARCHAR(100),
    decided_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_onboarding_decision_onboarding FOREIGN KEY (onboarding_id) REFERENCES agent_onboarding(onboarding_id)
);