-- Seed initial admin user and base roles/permissions
-- Password: AdminPass123! (BCrypt hashed)

-- Insert admin user (password: AdminPass123!)
INSERT INTO users (
    user_id,
    username,
    email,
    password_hash,
    full_name,
    status,
    failed_login_attempts,
    password_changed_at,
    password_expires_at,
    created_at,
    updated_at,
    created_by
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    'admin@agentbanking.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System Administrator',
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '90 days',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'system'
) ON CONFLICT (username) DO NOTHING;

-- Insert base roles
INSERT INTO roles (role_id, role_name, description, is_active, created_at, updated_at, created_by) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'IT_ADMIN', 'IT Administrator with full system access', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('b0000000-0000-0000-0000-000000000002', 'BANK_OPERATOR', 'Bank operations staff', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('b0000000-0000-0000-0000-000000000003', 'AGENT', 'Field agent for transactions', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('b0000000-0000-0000-0000-000000000004', 'AUDITOR', 'Read-only auditor access', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('b0000000-0000-0000-0000-000000000005', 'TELLER', 'Branch teller for customer transactions', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system')
ON CONFLICT (role_name) DO NOTHING;

-- Insert base permissions
INSERT INTO permissions (permission_id, permission_key, description, resource, action, is_active, created_at, updated_at, created_by) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'user:read', 'Read user information', 'user', 'read', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000002', 'user:write', 'Create/update users', 'user', 'write', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000003', 'user:delete', 'Delete users', 'user', 'delete', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000004', 'role:read', 'Read role information', 'role', 'read', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000005', 'role:write', 'Create/update roles', 'role', 'write', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000006', 'transaction:create', 'Create transactions', 'transaction', 'create', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000007', 'transaction:read', 'Read transactions', 'transaction', 'read', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000008', 'ledger:read', 'Read ledger entries', 'ledger', 'read', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000009', 'ledger:write', 'Write ledger entries', 'ledger', 'write', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000010', 'audit:read', 'Read audit logs', 'audit', 'read', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system'),
    ('c0000000-0000-0000-0000-000000000011', 'kyc:verify', 'Perform KYC verification', 'kyc', 'verify', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system')
ON CONFLICT (permission_key) DO NOTHING;

-- Assign IT_ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Assign all permissions to IT_ADMIN role
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000004'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000005'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000006'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000007'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000008'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000009'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000010'),
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000011')
ON CONFLICT DO NOTHING;