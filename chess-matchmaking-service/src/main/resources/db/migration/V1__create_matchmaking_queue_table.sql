CREATE TABLE IF NOT EXISTS matchmaking_queue (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    rating INTEGER NOT NULL,
    time_control VARCHAR(20) NOT NULL,
    min_rating INTEGER,
    max_rating INTEGER,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING'
);

CREATE INDEX idx_matchmaking_queue_user_id ON matchmaking_queue(user_id);
CREATE INDEX idx_matchmaking_queue_rating ON matchmaking_queue(rating);
CREATE INDEX idx_matchmaking_queue_time_control ON matchmaking_queue(time_control);
CREATE INDEX idx_matchmaking_queue_status ON matchmaking_queue(status);
CREATE INDEX idx_matchmaking_queue_joined_at ON matchmaking_queue(joined_at);
