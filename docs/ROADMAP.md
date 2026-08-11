# SettleSense Backend & Product Roadmap

> **Last Updated:** August 2026  
> **Current Status:** Phase 1 Complete ✅ | Phase 2 In Progress 🚧

---

## Roadmap Overview & Learning Goals

This roadmap outlines both the product feature evolution of **SettleSense** and the accompanying engineering mastery goals. 

Every roadmap milestone requires:
1. **Domain & Technical Design**: Clear contracts, edge case handling, and data models.
2. **Robust Code & Tests**: Complete test coverage (`JUnit 5`, `Mockito`, `SpringBootTest`).
3. **Production Thinking**: System design discussions around concurrency, caching, rate limiting, and observability.

---

## Phase Status Summary

```mermaid
gantt
    title SettleSense Development Roadmap
    dateFormat  YYYY-MM-DD
    section Phase 1: Foundation
    Core Models & Ledger Engine     :done,    p1, 2026-06-01, 2026-06-21
    section Phase 2: Expense Engine
    Multi-Payer & Advanced Splits   :active,  p2, 2026-06-22, 2026-08-30
    section Phase 3: Auth & Security
    JWT & OAuth2 Integration       :planned, p3, 2026-09-01, 2026-10-15
    section Phase 4: AI & Insights
    Debt Assistant & Analytics      :planned, p4, 2026-10-16, 2026-11-30
    section Phase 5: Production & Cloud
    Docker, Metrics & Microservices :planned, p5, 2026-12-01, 2027-01-31
```

---

## Detailed Roadmap Phases

### Phase 1: Foundation & Core Financial Engine ✅ (COMPLETE)

- [x] **1.1 Flyway Database Schema & Entities**:
  - `User`, `Group`, `GroupMember`, `Expense`, `ExpenseSplit`, `Settlement`, `Friendship`, `ActivityEvent`, `LedgerEntry`, `BalanceProjection`.
  - Integer minor units (`Long totalMinor` in paise) for zero floating-point errors.
- [x] **1.2 User Management Workflow**:
  - User registration, profile lookup, and status management.
- [x] **1.3 Group Management Workflow**:
  - Group creation, currency locking (`INR`), member roles (`OWNER`, `MEMBER`), group archiving.
- [x] **1.4 Expense Creation & Split Calculation**:
  - `EQUAL`, `EXACT`, `PERCENTAGE`, and `SHARES` split strategy calculations in `SplitCalculator`.
  - Remainder paise distribution to maintain exact zero-sum totals.
- [x] **1.5 Double-Entry Ledger & Balance Projections**:
  - Immediate ledger entry generation for expenses and settlements.
  - Pairwise net balance calculations stored in `BalanceProjection`.
- [x] **1.6 Settlement Recording & Greedy Simplification**:
  - Direct Payer $\rightarrow$ Payee settlement posting.
  - Greedy debt minimization algorithm reducing debt paths to $N-1$ transfers.

---

### Phase 2: Enhanced Expense Engine & Multi-Payer Splits 🚧 (IN PROGRESS)

- [x] **2.1 Redis-Backed Rate Limiting**:
  - Redis token bucket rate limiting on targeted read endpoints (ADR-009).
- [ ] **2.2 Multi-Payer Expense Engine**:
  - Support expenses paid by multiple group members (e.g. User A paid ₹600, User B paid ₹400 for a ₹1000 dinner).
  - Multi-payer ledger entry generation.
- [ ] **2.3 In-Place Expense Edits via Reversing Log**:
  - Support modifying description, amount, or splits without breaking immutability.
  - Automatic creation of compensating reversing entries followed by new replacement ledger entries.
- [ ] **2.4 Itemized Bill Splitting & Receipt Metadata**:
  - Support line-item splitting (e.g. Item 1 shared by A & B, Item 2 shared by C).
  - Storage for receipt attachments and image metadata.
- [ ] **2.5 Expense Category & Currency Exchange Integration**:
  - Expense taxonomy (`Food`, `Travel`, `Utilities`, `Rent`).
  - Multi-currency support with dynamic rate lookup for international trips.

---

### Phase 3: Authentication, Authorization & Security 📅 (PLANNED)

- [ ] **3.1 JWT Authentication Engine**:
  - User registration with password hashing (`BCryptPasswordEncoder`).
  - Access Token (JWT) & Refresh Token issuance with Redis token revocation.
- [ ] **3.2 Fine-Grained Role-Based Access Control (RBAC)**:
  - Security filter chain enforcing group-level access rules (Only `OWNER` can archive; only participants can view group expenses).
- [ ] **3.3 OAuth2 Social Login**:
  - Google / GitHub OAuth2 single sign-on flow.
- [ ] **3.4 Audit Trail & Security Headers**:
  - Rate limiting on Auth endpoints to prevent brute-force attacks.
  - OWASP Top 10 header protections (CORS, CSP, X-Frame-Options).

---

### Phase 4: Intelligent Settlement & AI Assistant 📅 (PLANNED)

- [ ] **4.1 AI Natural Language Expense Parser**:
  - Parse unstructured text (e.g. *"Spent 1200 on groceries paid by Rahul split equally with Amit and Priya"*) into structured split payloads.
- [ ] **4.2 Smart Debt Settlement Assistant**:
  - Contextual settlement recommendations via preferred payment channels (UPI / Bank transfer links).
- [ ] **4.3 Spending Analytics & Anomaly Detection**:
  - Monthly group spend trends, category distribution graphs, and spend anomaly alerts (e.g., unexpected spike in utility costs).

---

### Phase 5: Production Readiness, Observability & Cloud 📅 (PLANNED)

- [ ] **5.1 Dockerization & Multi-Environment Packaging**:
  - Production `Dockerfile` multi-stage build optimization for Spring Boot and Vite.
  - Production `docker-compose.prod.yml`.
- [ ] **5.2 Observability & Metrics**:
  - Spring Boot Actuator + Micrometer + Prometheus metrics collection.
  - Grafana dashboard template for HTTP throughput, DB connection pools, and ledger latency.
- [ ] **5.3 CI/CD Automation**:
  - GitHub Actions pipeline for linting, testing, Docker image building, and vulnerability scans.
- [ ] **5.4 Performance & Load Testing**:
  - Gatling / k6 load test scripts simulating 10,000 concurrent expense creations and balance projections.

---

## Engineering Maturity Milestones

| Milestone | Skill Focus | Target Deliverable |
| :--- | :--- | :--- |
| **M1** | Financial Ledger & Domain Modeling | Integer minor unit handling, double-entry ledger, remainder distribution |
| **M2** | Graph Algorithms & Debt Simplification | Greedy flow algorithm for minimal settlements |
| **M3** | Security & Auth | Spring Security 6, JWT, Claims validation, RBAC |
| **M4** | Distributed Systems & Caching | Redis rate limiting, cache eviction strategies |
| **M5** | AI Integration & Prompting | Natural language expense parsing & insight service |
