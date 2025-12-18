-- Make password_hash nullable to support OAuth users who don't have passwords

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;
