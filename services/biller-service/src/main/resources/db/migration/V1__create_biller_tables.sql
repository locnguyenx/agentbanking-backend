CREATE TABLE biller_config (
    biller_id UUID PRIMARY KEY,
    biller_code VARCHAR(20) NOT NULL UNIQUE,
    biller_name VARCHAR(100) NOT NULL,
    biller_type VARCHAR(20) NOT NULL,
    api_endpoint VARCHAR(500),
    api_key VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biller_code ON biller_config(biller_code);

CREATE TABLE bill_payment (
    payment_id UUID PRIMARY KEY,
    biller_id UUID NOT NULL REFERENCES biller_config(biller_id),
    internal_transaction_id UUID NOT NULL,
    ref1 VARCHAR(50),
    ref2 VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    receipt_no VARCHAR(50),
    biller_reference VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_payment_biller ON bill_payment(biller_id);
CREATE INDEX idx_payment_internal_tx ON bill_payment(internal_transaction_id);

CREATE TABLE topup_transaction (
    topup_id UUID PRIMARY KEY,
    internal_transaction_id UUID NOT NULL,
    telco VARCHAR(20) NOT NULL,
    phone_number VARCHAR(15) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    telco_reference VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_topup_telco ON topup_transaction(telco);
CREATE INDEX idx_topup_phone ON topup_transaction(phone_number);
