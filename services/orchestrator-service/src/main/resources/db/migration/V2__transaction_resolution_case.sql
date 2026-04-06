CREATE TABLE transaction_resolution_case (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    proposed_action VARCHAR(20),
    reason_code VARCHAR(50),
    reason TEXT,
    evidence_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_MAKER',
    maker_user_id VARCHAR(128),
    maker_created_at TIMESTAMP,
    checker_user_id VARCHAR(128),
    checker_action VARCHAR(20),
    checker_reason TEXT,
    checker_completed_at TIMESTAMP,
    temporal_signal_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resolution_case_status ON transaction_resolution_case(status);
CREATE INDEX idx_resolution_case_workflow ON transaction_resolution_case(workflow_id);
