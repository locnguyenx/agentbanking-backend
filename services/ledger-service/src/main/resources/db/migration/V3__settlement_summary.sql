CREATE TABLE settlement_summary (
    settlement_id UUID PRIMARY KEY,
    agent_id UUID NOT NULL,
    settlement_date DATE NOT NULL,
    total_withdrawals DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_deposits DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_bill_payments DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_retail_sales DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_commissions DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    direction VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'MYR',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_agent_id ON settlement_summary(agent_id);
CREATE INDEX idx_settlement_date ON settlement_summary(settlement_date);
CREATE UNIQUE INDEX idx_settlement_agent_date ON settlement_summary(agent_id, settlement_date);
