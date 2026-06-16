# SettleSense - High-Level Instructions

SettleSense is a smarter clone of Splitwise - a group expense management application where users can register, add friends, create groups, and track expenses with settlement capabilities.

## Project Overview

SettleSense is organized as a multi-module monorepo with the following structure:

```
SettleSense/
├── backend/       # Spring Boot API (Java 17+, Gradle)
├── frontend/      # React + TypeScript + Vite
├── docker/        # Local infrastructure (Postgres)
├── docs/          # Project documentation
└── install.cmd    # Claude Code installer (Windows)
```

## Quick Start

### Prerequisites
- **Backend**: Java 17+, Gradle, PostgreSQL 16
- **Frontend**: Node.js 18+, npm
- **Docker**: Docker Desktop

### Starting the Application

1. **Start Infrastructure (PostgreSQL)**:
   ```powershell
   docker compose -f docker/docker-compose.yml up -d
   ```

2. **Run Backend**:
   ```powershell
   cd backend
   .\gradlew.bat bootRun
   ```
   - Backend runs on: `http://localhost:8080`
   - Database: PostgreSQL on `localhost:5432` (settlesense/settlesense)

3. **Run Frontend**:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
   - Frontend runs on: `http://localhost:5173` (Vite dev server)
   - API proxy: `/api` → `http://localhost:8080`

### Running Tests

**Backend**:
```powershell
cd backend
.\gradlew.bat test
```

**Frontend**:
```powershell
cd frontend
npm run lint
npm run build
```

## Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x with Spring Security
- **Database**: PostgreSQL with Flyway migrations
- **API Style**: RESTful JSON APIs
- **Security**: CSRF disabled, permitAll for `/api/**`

### Frontend (React)
- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite
- **Routing**: Client-side routing with React Router pattern
- **Styling**: CSS

### Database
- **PostgreSQL 16** (via Docker)
- **Flyway** for migrations
- Database credentials: `settlesense` (database, username, password)

## Key Features Implemented

### Phase 1 Features
- ✅ User registration
- ✅ Group creation and management
- ✅ Group membership (add, remove, leave)
- ✅ Equal expense posting
- ✅ Balance projection calculation
- ✅ Settlement recording
- ✅ Settlement suggestions (simplified settlements)
- ✅ Ledger-based balance system

### Domain Model
- **Users**: People who can participate in groups and expenses
- **Groups**: Shared contexts (trips, households, events)
- **GroupMembers**: User memberships in groups with roles (OWNER, MEMBER)
- **Expenses**: Bills paid by one user, split among members
- **ExpenseSplits**: How expenses are divided (EQUAL, EXACT, PERCENTAGE, SHARE)
- **Settlements**: Actual payments between users
- **LedgerEntries**: Normalized money movements (source of truth)
- **BalanceProjections**: Derived outstanding balances
- **ActivityEvents**: Audit trail

## Known Limitations (Phase 1)

- **Single payer**: Expenses can only have one payer
- **No authentication**: No user login/authentication system
- **No friendships**: Friend system not yet implemented
- **Limited split types**: Only EQUAL split fully working
- **No expense editing**: Cancellation supported, but not in-place editing
- **No mobile app**: Web-only for now

## Future Development

### Phase 2 Priorities
1. User authentication (JWT or OAuth)
2. Multi-payer expenses
3. Expense editing (not just cancellation)
4. Extended split types (EXACT, PERCENTAGE, SHARE)
5. Friendship system
6. Activity feed improvements
7. Mobile app (React Native or Flutter)

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/users` | POST | Register new user |
| `/api/users` | GET | List all users |
| `/api/users/{id}` | GET | Get user by ID |
| `/api/groups` | POST | Create group |
| `/api/groups` | GET | List all groups |
| `/api/groups/{id}` | GET | Get group by ID |
| `/api/groups/{id}/members` | GET | List group members |
| `/api/groups/{id}/members` | POST | Add member |
| `/api/groups/{id}/expenses` | POST | Post expense |
| `/api/groups/{id}/settlements` | POST | Record settlement |
| `/api/groups/{id}/balances` | GET | Get balances |
| `/api/groups/{id}/settlement-suggestions` | GET | Get suggestions |

## Project Documentation

- `docs/development.md` - Detailed setup and development commands
- `docs/phase-1-domain-model-and-money-rules.md` - Domain model specification and money handling rules

## Troubleshooting

### Database Connection Issues
- Ensure Docker is running: `docker ps`
- Check Postgres logs: `docker logs settlesense-postgres`
- Verify credentials match `application.properties`

### Backend Won't Start
- Check Java version: `java -version` (needs 17+)
- Check Gradle: `./gradlew.bat --version`
- Ensure database is running first

### Frontend Issues
- Clear node_modules: `rm -rf node_modules` and reinstall
- Check Node version: `node --version` (needs 18+)