CREATE TABLE IF NOT EXISTS game_states (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT UNIQUE NOT NULL,
    current_fen VARCHAR(100) NOT NULL,
    current_turn VARCHAR(5) NOT NULL,
    move_count INTEGER DEFAULT 0,
    white_time_left INTEGER NOT NULL,
    black_time_left INTEGER NOT NULL,
    last_move_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_states_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE INDEX idx_game_states_game_id ON game_states(game_id);
