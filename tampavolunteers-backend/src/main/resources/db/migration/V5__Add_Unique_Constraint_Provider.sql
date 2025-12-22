-- V5: Add unique constraint on provider_id + auth_provider combination
-- This ensures we don't get duplicate OAuth users from the same provider

-- First, clean up any duplicate entries (keep the most recent one)
-- This handles the case where duplicates were created during testing
DELETE FROM users u1
USING users u2
WHERE u1.id < u2.id
  AND u1.provider_id = u2.provider_id
  AND u1.auth_provider = u2.auth_provider
  AND u1.provider_id IS NOT NULL
  AND u1.auth_provider IS NOT NULL;

-- Add a unique constraint on the combination of provider_id and auth_provider
-- This prevents future duplicates
CREATE UNIQUE INDEX idx_unique_provider_auth 
ON users (provider_id, auth_provider) 
WHERE provider_id IS NOT NULL AND auth_provider IS NOT NULL;

-- Add a comment explaining the constraint
COMMENT ON INDEX idx_unique_provider_auth IS 'Ensures unique OAuth provider ID per authentication provider (prevents duplicate GitHub/Google users)';
