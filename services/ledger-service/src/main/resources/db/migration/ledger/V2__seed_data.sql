-- Seed data for ledger service (runs after schema migration)
-- This file uses Flyway's repeatable migration pattern with IF NOT EXISTS

-- Agent Floats (only insert if empty)
INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
SELECT 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 125000.00, 5000.00, 'MYR', 0, '2026-03-26 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE float_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11');

INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
SELECT 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 85000.00, 2000.00, 'MYR', 0, '2026-03-26 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE float_id = 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22');

INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
SELECT 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 250000.00, 10000.00, 'MYR', 0, '2026-03-26 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE float_id = 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33');

INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
SELECT 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 45000.00, 0.00, 'MYR', 0, '2026-03-26 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE float_id = 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44');

INSERT INTO agent_float (float_id, agent_id, balance, reserved_balance, currency, version, updated_at)
SELECT 'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 180000.00, 8000.00, 'MYR', 0, '2026-03-26 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM agent_float WHERE float_id = 'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55');

-- Transactions
INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a61', 'idem-001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CASH_DEPOSIT', 5000.00, 0.00, 50.00, 100.00, 'COMPLETED', '411111******1111', '2026-03-26 08:15:00', '2026-03-26 08:15:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a61');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f6eebc99-9c0b-4ef8-bb6d-6bb9bd380a62', 'idem-002', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'CASH_WITHDRAWAL', 2500.00, 2.50, 25.00, 50.00, 'COMPLETED', '411111******2222', '2026-03-26 08:45:00', '2026-03-26 08:45:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f6eebc99-9c0b-4ef8-bb6d-6bb9bd380a62');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f7eebc99-9c0b-4ef8-bb6d-6bb9bd380a63', 'idem-003', 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'CASH_DEPOSIT', 8000.00, 0.00, 80.00, 160.00, 'COMPLETED', '411111******3333', '2026-03-26 09:00:00', '2026-03-26 09:00:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f7eebc99-9c0b-4ef8-bb6d-6bb9bd380a63');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f8eebc99-9c0b-4ef8-bb6d-6bb9bd380a64', 'idem-004', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CASH_WITHDRAWAL', 1000.00, 1.50, 10.00, 20.00, 'PENDING', '411111******4444', '2026-03-26 09:15:00', NULL
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f8eebc99-9c0b-4ef8-bb6d-6bb9bd380a64');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f9eebc99-9c0b-4ef8-bb6d-6bb9bd380a65', 'idem-005', 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'CASH_DEPOSIT', 15000.00, 0.00, 150.00, 300.00, 'COMPLETED', '411111******5555', '2026-03-26 10:00:00', '2026-03-26 10:00:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f9eebc99-9c0b-4ef8-bb6d-6bb9bd380a65');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66', 'idem-006', 'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 'CASH_WITHDRAWAL', 3000.00, 3.00, 30.00, 60.00, 'COMPLETED', '411111******6666', '2026-03-26 10:30:00', '2026-03-26 10:30:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a66');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a67', 'idem-007', 'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'CASH_DEPOSIT', 12000.00, 0.00, 120.00, 240.00, 'COMPLETED', '411111******7777', '2026-03-26 11:00:00', '2026-03-26 11:00:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380a67');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f2eebc99-9c0b-4ef8-bb6d-6bb9bd380a68', 'idem-008', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CASH_WITHDRAWAL', 5000.00, 5.00, 50.00, 100.00, 'COMPLETED', '411111******8888', '2026-03-26 11:30:00', '2026-03-26 11:30:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f2eebc99-9c0b-4ef8-bb6d-6bb9bd380a68');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f3eebc99-9c0b-4ef8-bb6d-6bb9bd380a69', 'idem-009', 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'CASH_WITHDRAWAL', 15000.00, 15.00, 150.00, 300.00, 'COMPLETED', '411111******9999', '2026-03-26 12:00:00', '2026-03-26 12:00:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f3eebc99-9c0b-4ef8-bb6d-6bb9bd380a69');

INSERT INTO ledger_transaction (transaction_id, idempotency_key, agent_id, transaction_type, amount, customer_fee, agent_commission, bank_share, status, customer_card_masked, created_at, completed_at)
SELECT 'f4eebc99-9c0b-4ef8-bb6d-6bb9bd380a70', 'idem-010', 'd3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'CASH_DEPOSIT', 25000.00, 0.00, 250.00, 500.00, 'COMPLETED', '411111******0000', '2026-03-26 12:30:00', '2026-03-26 12:30:30'
WHERE NOT EXISTS (SELECT 1 FROM ledger_transaction WHERE transaction_id = 'f4eebc99-9c0b-4ef8-bb6d-6bb9bd380a70');

-- Journal Entries
INSERT INTO journal_entry (journal_id, transaction_id, entry_type, account_code, amount, description, created_at)
SELECT '10000000-0000-0000-0000-000000000001', 'f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a61', 'DEBIT', '2000-CASH', 5000.00, 'Cash deposit received', '2026-03-26 08:15:00'
WHERE NOT EXISTS (SELECT 1 FROM journal_entry WHERE journal_id = '10000000-0000-0000-0000-000000000001');

INSERT INTO journal_entry (journal_id, transaction_id, entry_type, account_code, amount, description, created_at)
SELECT '10000000-0000-0000-0000-000000000002', 'f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a61', 'CREDIT', '1000-FLOAT', 5000.00, 'Agent float credited', '2026-03-26 08:15:00'
WHERE NOT EXISTS (SELECT 1 FROM journal_entry WHERE journal_id = '10000000-0000-0000-0000-000000000002');

INSERT INTO journal_entry (journal_id, transaction_id, entry_type, account_code, amount, description, created_at)
SELECT '10000000-0000-0000-0000-000000000003', 'f6eebc99-9c0b-4ef8-bb6d-6bb9bd380a62', 'DEBIT', '1000-FLOAT', 2500.00, 'Agent float debited', '2026-03-26 08:45:00'
WHERE NOT EXISTS (SELECT 1 FROM journal_entry WHERE journal_id = '10000000-0000-0000-0000-000000000003');

INSERT INTO journal_entry (journal_id, transaction_id, entry_type, account_code, amount, description, created_at)
SELECT '10000000-0000-0000-0000-000000000004', 'f6eebc99-9c0b-4ef8-bb6d-6bb9bd380a62', 'CREDIT', '2000-CASH', 2500.00, 'Cash withdrawal disbursed', '2026-03-26 08:45:00'
WHERE NOT EXISTS (SELECT 1 FROM journal_entry WHERE journal_id = '10000000-0000-0000-0000-000000000004');