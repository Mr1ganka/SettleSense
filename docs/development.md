# SettleSense Development

## Layout

- `backend/` - Spring Boot API, Gradle wrapper, Flyway migrations.
- `frontend/` - React, TypeScript, Vite.
- `docker/` - Local infrastructure.
- `docs/` - Project notes and setup guides.

## Local Infrastructure

Start Postgres:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Stop Postgres:

```powershell
docker compose -f docker/docker-compose.yml down
```

## Backend

Run tests:

```powershell
cd backend
.\gradlew.bat test
```

Run API:

```powershell
cd backend
.\gradlew.bat bootRun
```

The default API config expects Postgres on `localhost:5432` with database, username, and password all set to `settlesense`.

## Frontend

Install dependencies:

```powershell
cd frontend
npm install
```

Run checks:

```powershell
npm run lint
npm run build
```

Run dev server:

```powershell
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`.
