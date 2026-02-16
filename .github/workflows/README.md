# Chess Platform — CI/CD

## Workflows

| Workflow | Trigger | Что делает |
|----------|--------|------------|
| **CI** | Push / PR в `main` | Backend: Maven verify (unit + IT). Frontend: lint, build. Docker: проверка `docker-compose` и сборка одного backend + frontend образа. |
| **Docker Build & Push** | После успешного CI на `main`, push тега `v*`, или ручной запуск | Собирает и пушит все 8 сервисных образов в GitHub Container Registry (ghcr.io). |

## Образы

Образы публикуются в **GitHub Container Registry**:

- `ghcr.io/<owner>/chess-auth-service`
- `ghcr.io/<owner>/chess-user-service`
- `ghcr.io/<owner>/chess-matchmaking-service`
- `ghcr.io/<owner>/chess-game-service`
- `ghcr.io/<owner>/chess-ws-service`
- `ghcr.io/<owner>/chess-analytics-service`
- `ghcr.io/<owner>/chess-api-gateway`
- `ghcr.io/<owner>/chess-frontend`

Теги: `latest` (только для `main`), короткий SHA коммита, имя ветки, для тегов `v*` — semver.

## Оптимизации

- **Concurrency**: повторный push/PR отменяет предыдущий запуск для той же ветки.
- **Кэш**: Maven (`.m2`), npm, Docker layers (GitHub Actions cache).
- **paths-ignore**: CI не запускается при изменении только `*.md`, `docs/`, `.gitignore`.

## Ручной запуск

- **Docker Build & Push**: вкладка Actions → "Docker Build & Push" → "Run workflow".

## Релиз по тегу

Чтобы собрать и опубликовать образы по версии:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Образы получат тег `1.0.0` (semver).
