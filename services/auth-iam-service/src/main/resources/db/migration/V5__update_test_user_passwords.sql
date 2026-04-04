-- Update seed/test user passwords to "12345678" (BCrypt hashed)
-- Admin user (username = 'admin') keeps password "password"
-- All other users get password "12345678" (minimum 8 chars required by validation)

-- BCrypt hash for "12345678": $2b$12$WcDQsraPcIfwfeOgBvj8z.abeiL0.oG6dht5o98D49gmiwqbTO3Ie

UPDATE users
SET password_hash = '$2b$12$WcDQsraPcIfwfeOgBvj8z.abeiL0.oG6dht5o98D49gmiwqbTO3Ie',
    password_changed_at = CURRENT_TIMESTAMP,
    must_change_password = false
WHERE username != 'admin'
  AND username LIKE 'AGT-%';
