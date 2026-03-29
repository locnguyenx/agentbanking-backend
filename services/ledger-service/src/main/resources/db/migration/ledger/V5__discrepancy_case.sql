CREATE TABLE discrepancy_case (
    case_id UUID PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    discrepancy_type VARCHAR(20) NOT NULL,
    internal_amount DECIMAL(15,2),
    network_amount DECIMAL(15,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_MAKER',
    maker_action VARCHAR(50),
    maker_user_id VARCHAR(50),
    maker_reason TEXT,
    checker_user_id VARCHAR(50),
    checker_action VARCHAR(50),
    checker_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX idx_discrepancy_status ON discrepancy_case(status);
CREATE INDEX idx_discrepancy_type ON discrepancy_case(discrepancy_type);
