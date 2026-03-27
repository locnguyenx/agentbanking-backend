CREATE TABLE agent (
    agent_id UUID PRIMARY KEY,
    agent_code VARCHAR(20) UNIQUE NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    merchant_gps_lat DECIMAL(9,6) NOT NULL,
    merchant_gps_lng DECIMAL(9,6) NOT NULL,
    mykad_number VARCHAR(255) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_mykad ON agent(mykad_number);
CREATE INDEX idx_agent_status ON agent(status);
CREATE INDEX idx_agent_code ON agent(agent_code);
