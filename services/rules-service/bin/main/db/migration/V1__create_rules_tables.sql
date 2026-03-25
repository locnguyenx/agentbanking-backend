CREATE TABLE fee_config (
    fee_config_id UUID PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    agent_tier VARCHAR(20) NOT NULL,
    fee_type VARCHAR(20) NOT NULL,
    customer_fee_value DECIMAL(15,4) NOT NULL,
    agent_commission_value DECIMAL(15,4) NOT NULL,
    bank_share_value DECIMAL(15,4) NOT NULL,
    daily_limit_amount DECIMAL(15,2),
    daily_limit_count INTEGER,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_fee_config UNIQUE (transaction_type, agent_tier, effective_from)
);

CREATE INDEX idx_fee_config_lookup ON fee_config(transaction_type, agent_tier, effective_from);

CREATE TABLE velocity_rule (
    rule_id UUID PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    max_transactions_per_day INTEGER NOT NULL,
    max_amount_per_day DECIMAL(15,2) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_velocity_active ON velocity_rule(is_active);
