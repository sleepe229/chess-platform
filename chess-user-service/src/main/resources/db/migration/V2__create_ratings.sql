CREATE TABLE ratings (
     id UUID PRIMARY KEY,
     user_id UUID NOT NULL,
     time_control VARCHAR(50) NOT NULL,
     rating DOUBLE PRECISION NOT NULL DEFAULT 1500.0,
     rating_deviation DOUBLE PRECISION NOT NULL DEFAULT 350.0,
     volatility DOUBLE PRECISION NOT NULL DEFAULT 0.06,
     games_played INTEGER NOT NULL DEFAULT 0,
     peak_rating DOUBLE PRECISION NOT NULL DEFAULT 1500.0,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     CONSTRAINT uq_ratings_user_time UNIQUE (user_id, time_control)
);

CREATE INDEX idx_ratings_user_id ON ratings(user_id);
CREATE INDEX idx_ratings_time_control ON ratings(time_control);
CREATE INDEX idx_ratings_rating ON ratings(rating DESC);
