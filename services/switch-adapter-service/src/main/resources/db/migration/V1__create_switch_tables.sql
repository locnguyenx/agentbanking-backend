CREATE TABLE switch_transaction (
    switch_tx_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    mt_type VARCHAR(10) NOT NULL,
    iso_response_code VARCHAR(5),
    switch_reference VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    original_reference VARCHAR(50),
    reversal_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_switch_tx_internal ON switch_transaction(internal_transaction_id);
CREATE INDEX idx_switch_tx_status ON switch_transaction(status);

CREATE TABLE reversal_queue (
    queue_id UUID PRIMARY KEY,
    original_transaction_id UUID NOT NULL,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP
);

CREATE INDEX idx_reversal_status ON reversal_queue(status);
