CREATE TABLE IF NOT EXISTS match_history (
    id BIGSERIAL PRIMARY KEY,
    player1_id BIGINT NOT NULL,
    player2_id BIGINT NOT NULL,
    game_id BIGINT,
    time_control VARCHAR(20) NOT NULL,
    matched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_history_player1 ON match_history(player1_id);
CREATE INDEX idx_match_history_player2 ON match_history(player2_id);
CREATE INDEX idx_match_history_game_id ON match_history(game_id);
CREATE INDEX idx_match_history_matched_at ON match_history(matched_at);
