CREATE TABLE IF NOT EXISTS moves (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    move_number INTEGER NOT NULL,
    move_notation VARCHAR(10) NOT NULL,
    fen_after_move VARCHAR(100) NOT NULL,
    time_left_white INTEGER NOT NULL,
    time_left_black INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moves_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE INDEX idx_moves_game_id ON moves(game_id);
CREATE INDEX idx_moves_game_move_number ON moves(game_id, move_number);
