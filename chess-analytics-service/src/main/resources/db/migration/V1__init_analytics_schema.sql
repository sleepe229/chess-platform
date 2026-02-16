-- Analytics read model (CQRS)
CREATE SCHEMA IF NOT EXISTS chess_analytics;

CREATE TABLE chess_analytics.game_facts (
    game_id         UUID PRIMARY KEY,
    white_player_id UUID NOT NULL,
    black_player_id UUID NOT NULL,
    result          VARCHAR(32) NOT NULL,
    finish_reason   VARCHAR(64) NOT NULL,
    winner_id       UUID,
    finished_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    pgn             TEXT,
    rated           BOOLEAN NOT NULL DEFAULT true,
    time_control_type VARCHAR(32) NOT NULL,
    move_count      INT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_facts_white ON chess_analytics.game_facts(white_player_id);
CREATE INDEX idx_game_facts_black ON chess_analytics.game_facts(black_player_id);
CREATE INDEX idx_game_facts_finished_at ON chess_analytics.game_facts(finished_at DESC);

-- In-progress game stub (from GameStarted) to accumulate move_count until GameFinished
CREATE TABLE chess_analytics.game_progress (
    game_id    UUID PRIMARY KEY,
    move_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chess_analytics.player_rating_snapshots (
    id         BIGSERIAL PRIMARY KEY,
    player_id  UUID NOT NULL,
    time_control VARCHAR(32) NOT NULL,
    rating     DOUBLE PRECISION NOT NULL,
    game_id    UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_player_rating_player ON chess_analytics.player_rating_snapshots(player_id);
CREATE INDEX idx_player_rating_updated ON chess_analytics.player_rating_snapshots(player_id, time_control, updated_at DESC);

CREATE TABLE chess_analytics.analysis_jobs (
    analysis_job_id  UUID PRIMARY KEY,
    game_id          UUID NOT NULL,
    requested_by     UUID NOT NULL,
    status           VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_moves      INT,
    accuracy_white   INT,
    accuracy_black   INT,
    error_message    TEXT,
    completed_at     TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analysis_jobs_game ON chess_analytics.analysis_jobs(game_id);
CREATE INDEX idx_analysis_jobs_requested_by ON chess_analytics.analysis_jobs(requested_by);
CREATE INDEX idx_analysis_jobs_status ON chess_analytics.analysis_jobs(status);
