ALTER TABLE users ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN user_status VARCHAR(50) NOT NULL DEFAULT 'VOLUNTEER';
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

CREATE INDEX idx_users_is_public ON users(is_public);
CREATE INDEX idx_users_user_status ON users(user_status);
CREATE INDEX idx_users_last_login_at ON users(last_login_at);
