# SettleSense - Documentation & System Portal

Welcome to the **SettleSense** master documentation hub. SettleSense is an enterprise-grade, high-precision group expense tracking, balance calculation, and debt settlement engine built with Spring Boot, PostgreSQL, and React.

---

## 🚀 Quick Navigation & Documentation Map

| Document | Description | Key Focus |
| :--- | :--- | :--- |
| 📖 **[Project Flow](file:///C:/Users/kelvin/Documents/SettleSense/docs/FLOW.md)** | End-to-end functional & system architecture flows | System diagrams, sequences, debt minimization, financial flows |
| 🛠️ **[Engineering Instructions](file:///C:/Users/kelvin/Documents/SettleSense/docs/INSTRUCTIONS.md)** | Coding standards, architectural rules, & guidelines | Layer responsibilities, money precision rules, Flyway, testing |
| 🗺️ **[Product & Learning Roadmap](file:///C:/Users/kelvin/Documents/SettleSense/docs/ROADMAP.md)** | Phased feature implementation & learning plan | Phase 1 to Phase 5 status, design questions, implementation goals |
| 📐 **[Architecture Decisions (ADRs)](file:///C:/Users/kelvin/Documents/SettleSense/docs/DECISIONS.md)** | Technical decisions, trade-offs, and design rationale | ADR-001 through ADR-009 (Ledger, Paise units, Flyway, etc.) |
| ⚡ **[Development Guide](file:///C:/Users/kelvin/Documents/SettleSense/docs/development.md)** | Local environment setup & docker setup | Gradle commands, npm setup, Docker compose execution |
| 📊 **[Implementation Status](file:///C:/Users/kelvin/Documents/SettleSense/docs/IMPLEMENTATION_STATUS.md)** | Detailed audit of completed and remaining work | Directory-by-directory breakdown of codebase status |
| 💰 **[Phase 1 Domain Model & Money Rules](file:///C:/Users/kelvin/Documents/SettleSense/docs/phase-1-domain-model-and-money-rules.md)** | Core domain entities & monetary invariants | Integer minor units, ledger posting rules, cancellation audit |

---

## 🌟 Key Product Features

1. **High-Precision Money Engine**:
   - Zero floating-point drift. All monetary amounts are stored as integer minor units (Paise for INR).
   - Deterministic remainder handling during equal/percentage/shares splits.
2. **Double-Entry Style Audit Ledger**:
   - Immutable financial history.
   - Reversing entries for expense modifications or cancellations.
3. **Greedy Debt Minimization**:
   - Automatically simplifies $O(N^2)$ complex multi-person balances down to a minimal $N-1$ settlement transfer list.
4. **Multi-Module Monorepo**:
   - Clean separation of Spring Boot backend, Flyway schema migrations, and React TypeScript SPA frontend.

---

## 💻 Local Quick Start

### Prerequisites
- **Java 17+** (JDK)
- **Node.js 18+** & **npm 9+**
- **Docker Desktop** (with Docker Compose)

### 1. Start Infrastructure
```powershell
docker compose -f docker/docker-compose.yml up -d
```

### 2. Start Backend Service
```powershell
cd backend
.\gradlew.bat bootRun
```
*Backend runs on `http://localhost:8080` (Health check: `http://localhost:8080/api/health`).*

### 3. Start Frontend UI
```powershell
cd frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173`.*

---

## 🧪 Verification & Testing

```powershell
# Run backend test suite (Unit, Integration & Service tests)
cd backend
.\gradlew.bat test

# Run frontend linting & production build check
cd ..\frontend
npm run lint
npm run build
```
