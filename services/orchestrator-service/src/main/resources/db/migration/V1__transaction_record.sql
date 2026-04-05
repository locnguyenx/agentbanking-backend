CREATE TABLE transaction_record (
    id UUID PRIMARY KEY,
    workflow_id VARCHAR(128) NOT NULL UNIQUE,
    transaction_type VARCHAR(50) NOT NULL,
    agent_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    customer_fee DECIMAL(10,2),
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    external_reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_record_agent ON transaction_record(agent_id);
CREATE INDEX idx_txn_record_status ON transaction_record(status);
CREATE INDEX idx_txn_record_created ON transaction_record(created_at);
CREATE INDEX idx_txn_record_type ON transaction_record(transaction_type);