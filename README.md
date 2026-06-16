# SettleSense

SettleSense is organized as a Spring Boot backend, React frontend, local Docker infrastructure, and project docs.

## Quick Start

```powershell
docker compose -f docker/docker-compose.yml up -d

cd backend
.\gradlew.bat bootRun

cd ..\frontend
npm install
npm run dev
```

## Checks

```powershell
cd backend
.\gradlew.bat test

cd ..\frontend
npm run lint
npm run build
```

See `docs/development.md` for setup detail.

## Planning

- `docs/phase-1-domain-model-and-money-rules.md` - domain model, money rules, ledger rules, balance projection, settlement, audit, and cancellation behavior.
