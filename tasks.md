# TelcoEdge - Common Commands

## Database (Docker)
- `docker compose up -d` — Postgres on **localhost:5433** (not 5432; avoids clash with a local PostgreSQL service)
- Credentials match `subscriber-api/src/main/resources/application.yml` (`telcoedge` / `telcoedge_dev`)

## Build and Test
- `./gradlew build` — full build
- `./gradlew :subscriber-api:bootRun` — run subscriber-api locally (requires Postgres above)
- `./gradlew test` — run all tests

## Endpoints (when subscriber-api is running)
- 'http:localhost:8080/api/vi/ping'
- 'http://localhost:8080/actuator/health'