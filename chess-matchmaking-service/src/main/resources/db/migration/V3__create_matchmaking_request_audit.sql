CREATE TABLE IF NOT EXISTS matchmaking_request_audit (
    request_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    time_control_type VARCHAR(16) NOT NULL,
    base_seconds INTEGER NOT NULL,
    increment_seconds INTEGER NOT NULL,
    rated BOOLEAN NOT NULL,
    rating DOUBLE PRECISION,
    rating_deviation DOUBLE PRECISION,
    status VARCHAR(16) NOT NULL,
    matched_game_id UUID,
    cancel_reason VARCHAR(32),
    x_request_id VARCHAR(64),
    idempotency_key VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_matchmaking_request_audit_user_id ON matchmaking_request_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_matchmaking_request_audit_status ON matchmaking_request_audit(status);
CREATE INDEX IF NOT EXISTS idx_matchmaking_request_audit_created_at ON matchmaking_request_audit(created_at);
