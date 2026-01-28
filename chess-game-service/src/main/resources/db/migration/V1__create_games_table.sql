CREATE TABLE IF NOT EXISTS games (
    id BIGSERIAL PRIMARY KEY,
    white_player_id BIGINT NOT NULL,
    black_player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    result VARCHAR(20),
    time_control VARCHAR(20) NOT NULL,
    initial_time_seconds INTEGER NOT NULL,
    increment_seconds INTEGER NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_games_white_player ON games(white_player_id);
CREATE INDEX idx_games_black_player ON games(black_player_id);
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_created_at ON games(created_at);
