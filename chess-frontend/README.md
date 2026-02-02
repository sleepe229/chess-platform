# Chess Platform Frontend (React + Vite)

SPA frontend for the Chess Platform.

## Local dev (recommended)

1) Start backend (gateway + services). If you run everything via Docker Compose, the gateway is on `:8080`.

2) Start frontend:

```bash
cd chess-frontend
npm install
npm run dev
```

Vite proxies API calls (`/v1`, `/auth`, `/users`, `/matchmaking`, `/games`, `/analysis`, `/ws`) to `http://localhost:8080`, so you don’t need CORS locally.

## Docker (single origin)

`docker/docker-compose.yaml` includes `chess-frontend` (nginx) that serves the SPA and proxies API+WS to `chess-api-gateway`, so everything works from one origin.

After `docker compose up --build` open:
- `http://localhost:3000`

## Implemented routes (per spec)

- `/login`, `/register`
- `/lobby` (matchmaking)
- `/game/:gameId` (WS real-time)
- `/profile` (edit + ratings)
- `/games` (uses rating-history as a temporary history source)
- `/games/:gameId` (viewer via `GET /v1/games/:id/state`)

Notes:
- Analytics endpoints are not implemented in backend yet, so analysis UI is a placeholder.
