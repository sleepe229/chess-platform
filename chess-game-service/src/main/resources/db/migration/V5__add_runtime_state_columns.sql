-- Persist minimal runtime snapshot in Postgres for recovery/debug.
-- Active state is still in Redis, but these columns allow reconstructing clocks/FEN if Redis is lost.

ALTER TABLE games
    ADD COLUMN IF NOT EXISTS current_fen TEXT,
    ADD COLUMN IF NOT EXISTS white_ms BIGINT,
    ADD COLUMN IF NOT EXISTS black_ms BIGINT,
    ADD COLUMN IF NOT EXISTS last_move_at TIMESTAMP;

ALTER TABLE game_moves
    ADD COLUMN IF NOT EXISTS white_ms_after BIGINT,
    ADD COLUMN IF NOT EXISTS black_ms_after BIGINT;

