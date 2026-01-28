CREATE TABLE rating_history (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                time_control VARCHAR(50) NOT NULL,
                                old_rating DOUBLE PRECISION NOT NULL,
                                new_rating DOUBLE PRECISION NOT NULL,
                                rating_change DOUBLE PRECISION NOT NULL,
                                old_rd DOUBLE PRECISION NOT NULL,
                                new_rd DOUBLE PRECISION NOT NULL,
                                game_id UUID,
                                opponent_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                opponent_rating DOUBLE PRECISION,
                                result VARCHAR(20),
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rating_history_user_id ON rating_history(user_id, created_at DESC);
CREATE INDEX idx_rating_history_user_time ON rating_history(user_id, time_control, created_at DESC);
CREATE INDEX idx_rating_history_game_id ON rating_history(game_id);
