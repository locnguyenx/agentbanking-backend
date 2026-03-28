CREATE TABLE ewallet_transaction (
    transaction_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    wallet_provider VARCHAR(50) NOT NULL,
    wallet_id VARCHAR(100) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    wallet_reference VARCHAR(50),
    agent_reference VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE essp_transaction (
    transaction_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    essp_certificate_number VARCHAR(50),
    agent_reference VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);