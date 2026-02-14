# Chess Online Platform

Online chess: sign up, find a game by time control and rating, play in real time over WebSocket. Clocks, draw offers, resign. Backend is a set of Java services behind one gateway; frontend is a React SPA.

Backend is Java 21 and Spring Boot 4 with Spring Cloud Gateway. Each domain has its own service and Postgres database. Auth uses JWT (access + refresh). Redis holds active game state and matchmaking queues. NATS JetStream is used for events between services (e.g. user registered, match found, move made). Frontend is React 19, TypeScript, Vite, Tailwind, with a small Zustand store and react-chessboard for the board.

You need Docker and Docker Compose to run the whole thing. For local dev you’ll need Java 21, Maven (or the project’s `./mvnw`), Node 20+, and npm.

**Run with Docker.** From the repo root, create a `.env` (in `docker/` or project root). It should define at least `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `REDIS_HOST`, `REDIS_PORT`, `NATS_URL`; see the compose file and env defaults for the full list. Then:

```bash
docker compose -f docker/docker-compose.yaml --env-file docker/.env up -d --build
```

Open http://localhost:3000, register, go to the lobby, pick a time control and hit Play. When a match is found you’re taken to the game page. The app is served on port 3000; the API gateway is on 8080; auth, user, matchmaking, game, ws, and analytics each have their own port (8081–8086) if you need to hit them directly.

**Backend tests:** `./mvnw test` from the root. Single module: `./mvnw test -pl chess-auth-service -am`. Package without tests: `./mvnw package -DskipTests`.

**Frontend:** `cd chess-frontend`, `npm ci`, then `npm run build` or `npm run dev`. Lint with `npm run lint`.

**Services:** Auth handles register/login/refresh/logout and creates the user profile in the user service on signup. User service keeps profiles and ratings. Matchmaking queues players by time control and rating and publishes a match event when two are paired. Game service consumes that event, creates the game, keeps state in Redis, and handles moves and draw/resign. WS service exposes a WebSocket at `/ws/game/{gameId}`; it talks to the game service for state and moves and subscribes to game events to push updates. Analytics service subscribes to events for stats. The gateway does JWT checks and routes; only auth and WebSocket upgrade are allowed without a token.

Repo layout: `chess-api-gateway`, `chess-auth-service`, `chess-user-service`, `chess-matchmaking-service`, `chess-game-service`, `chess-ws-service`, `chess-analytics-service`, plus `chess-common` and `chess-event-contracts` for shared code and event DTOs, `chess-frontend` for the React app, and `docker/docker-compose.yaml` for running it all.

API is under `/v1`: auth (`/v1/auth/register`, login, refresh, logout), users (`/v1/users/me`, etc.), matchmaking (join, leave, status), games (state, move, resign, offer-draw, accept-draw). WebSocket URL is `ws://localhost:3000/ws/game/{gameId}` with the token in the query string or in the handshake. Authenticated requests need `Authorization: Bearer <access_token>`.
