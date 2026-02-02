-- Align schema with TECHNICAL SPECIFICATION (UUID-based identifiers).
-- Previous V1-V3 used BIGINT/BIGSERIAL and a different layout.

DROP TABLE IF EXISTS game_states;
DROP TABLE IF EXISTS moves;
DROP TABLE IF EXISTS games;

CREATE TABLE games (
    game_id UUID PRIMARY KEY,
    white_id UUID NOT NULL,
    black_id UUID NOT NULL,
    time_control_type VARCHAR(16) NOT NULL,
    base_seconds INT NOT NULL,
    increment_seconds INT NOT NULL,
    rated BOOLEAN NOT NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    result VARCHAR(8) NULL,
    finish_reason VARCHAR(32) NULL,
    pgn TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_games_white_id ON games(white_id);
CREATE INDEX idx_games_black_id ON games(black_id);
CREATE INDEX idx_games_created_at ON games(created_at);

CREATE TABLE game_moves (
    game_id UUID NOT NULL REFERENCES games(game_id) ON DELETE CASCADE,
    ply INT NOT NULL,
    uci VARCHAR(8) NOT NULL,
    san VARCHAR(16) NULL,
    fen_after TEXT NOT NULL,
    played_at TIMESTAMP NOT NULL,
    by_user_id UUID NOT NULL,
    PRIMARY KEY (game_id, ply)
);

CREATE INDEX idx_game_moves_by_user ON game_moves(by_user_id);
