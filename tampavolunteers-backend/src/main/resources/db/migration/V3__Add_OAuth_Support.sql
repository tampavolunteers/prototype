-- Add OAuth support to users table

ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(255);

-- Add indexes for faster OAuth lookups
CREATE INDEX idx_users_auth_provider ON users(auth_provider);
CREATE INDEX idx_users_provider_id ON users(provider_id);
