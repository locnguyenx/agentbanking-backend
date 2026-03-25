CREATE TABLE agent_float (
    float_id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_float_agent_id ON agent_float(agent_id);

CREATE TABLE ledger_transaction (
    transaction_id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    agent_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2),
    customer_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    agent_commission DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    bank_share DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_code VARCHAR(20),
    customer_mykad VARCHAR(12),
    customer_card_masked VARCHAR(19),
    switch_reference VARCHAR(50),
    geofence_lat DECIMAL(9,6),
    geofence_lng DECIMAL(9,6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_transaction_agent_id ON ledger_transaction(agent_id);
CREATE INDEX idx_transaction_status ON ledger_transaction(status);
CREATE INDEX idx_transaction_created_at ON ledger_transaction(created_at);

CREATE TABLE journal_entry (
    journal_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_transaction_id ON journal_entry(transaction_id);
