-- Support OAuth2 / social login: provider and provider user id
ALTER TABLE auth_users
    ADD COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_user_id VARCHAR(255) NULL;

-- password_hash nullable for users who sign in only via OAuth (no password set)
ALTER TABLE auth_users
    ALTER COLUMN password_hash DROP NOT NULL;

-- Unique constraint: one provider + provider_user_id
CREATE UNIQUE INDEX idx_auth_users_provider_sub ON auth_users (auth_provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL;

COMMENT ON COLUMN auth_users.auth_provider IS 'LOCAL, GOOGLE, GITHUB, etc.';
COMMENT ON COLUMN auth_users.provider_user_id IS 'User id from OAuth2 provider (e.g. Google sub).';
