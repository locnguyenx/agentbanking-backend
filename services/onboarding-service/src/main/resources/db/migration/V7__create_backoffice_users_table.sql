-- Create backoffice users table
CREATE TABLE backoffice_users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(200),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_by VARCHAR(100)
);

-- Create user permissions table
CREATE TABLE user_permissions (
    user_id UUID NOT NULL,
    permission VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, permission),
    FOREIGN KEY (user_id) REFERENCES backoffice_users(user_id) ON DELETE CASCADE
);

-- Create index on username for faster lookups
CREATE INDEX idx_backoffice_users_username ON backoffice_users(username);
CREATE INDEX idx_backoffice_users_role ON backoffice_users(role);
CREATE INDEX idx_backoffice_users_status ON backoffice_users(status);
