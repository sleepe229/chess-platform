CREATE TABLE users (
   id UUID PRIMARY KEY,
   username VARCHAR(50) NOT NULL UNIQUE,
   email VARCHAR(255) NOT NULL UNIQUE,
   avatar_url VARCHAR(500),
   bio VARCHAR(500),
   country VARCHAR(100),
   total_games INTEGER NOT NULL DEFAULT 0,
   total_wins INTEGER NOT NULL DEFAULT 0,
   total_losses INTEGER NOT NULL DEFAULT 0,
   total_draws INTEGER NOT NULL DEFAULT 0,
   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
