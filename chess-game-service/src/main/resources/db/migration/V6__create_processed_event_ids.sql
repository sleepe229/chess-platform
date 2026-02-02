CREATE TABLE IF NOT EXISTS processed_event_ids (
    consumer VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (consumer, event_id)
);

