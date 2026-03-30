-- V3__add_user_type_and_password_policy.sql

-- Add user_type column with default INTERNAL
ALTER TABLE users ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

-- Add agent_id for EXTERNAL users
ALTER TABLE users ADD COLUMN agent_id UUID UNIQUE;

-- Add phone for OTP delivery
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- Add must_change_password flag
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;

-- Add temporary_password_expires_at
ALTER TABLE users ADD COLUMN temporary_password_expires_at TIMESTAMP WITH TIME ZONE;

-- Add constraint: agent_id only for EXTERNAL users
ALTER TABLE users ADD CONSTRAINT chk_agent_id_user_type
  CHECK (
    (user_type = 'EXTERNAL' AND agent_id IS NOT NULL) OR
    (user_type = 'INTERNAL' AND agent_id IS NULL)
  );

-- System parameters table
CREATE TABLE IF NOT EXISTS system_parameters (
    param_key VARCHAR(100) PRIMARY KEY,
    param_value VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default temp password expiry (3 days)
INSERT INTO system_parameters (param_key, param_value, description)
VALUES ('temp.password.expiry.days', '3', 'Temporary password expiry in days');

-- Add index for agent_id lookup
CREATE INDEX IF NOT EXISTS idx_users_agent_id ON users(agent_id);
