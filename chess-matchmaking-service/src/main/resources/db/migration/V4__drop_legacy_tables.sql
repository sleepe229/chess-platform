-- Legacy tables from early prototype (the service uses Redis-based queues now).
-- Keep DB schema aligned with actual implementation and avoid confusion.

DROP TABLE IF EXISTS match_history;
DROP TABLE IF EXISTS matchmaking_queue;

