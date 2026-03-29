-- Seed data for agents

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000001', 'AGT-001', 'Ahmad Razak Store', 'PREMIUM', 'ACTIVE', 3.139003, 101.686855, '860312017890', '012-3456789', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000001');

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000002', 'AGT-002', 'Siti Aminah Enterprise', 'STANDARD', 'ACTIVE', 3.073400, 101.606500, '930909028901', '013-8765432', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000002');

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000003', 'AGT-003', 'Faisal Trading', 'BASIC', 'SUSPENDED', 3.068500, 101.518200, '970505039012', '014-2468135', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000003');

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000004', 'AGT-004', 'Lee Ming Wei Retail', 'PREMIUM', 'ACTIVE', 5.414100, 100.328700, '850123011234', '016-9753124', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000004');

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000005', 'AGT-005', 'Nurul Huda Mini Mart', 'STANDARD', 'ACTIVE', 1.492700, 103.743100, '910101040123', '011-6543210', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000005');

INSERT INTO agent (agent_id, agent_code, business_name, tier, status, merchant_gps_lat, merchant_gps_lng, mykad_number, phone_number, created_at, updated_at)
SELECT 'a0000000-0000-0000-0000-000000000006', 'AGT-006', 'Tan Kah Seng Convenience', 'BASIC', 'ACTIVE', 4.597500, 101.092100, '920415055678', '017-1234567', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM agent WHERE agent_id = 'a0000000-0000-0000-0000-000000000006');
