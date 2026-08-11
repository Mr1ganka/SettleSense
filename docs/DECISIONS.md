# SettleSense - Architecture Decision Records (ADRs)

This document records the architectural and technical decisions made during the design and development of SettleSense, capturing context, alternatives considered, accepted trade-offs, and long-term implications.

---

## Decision Index

| ID | Title | Status | Date |
| :--- | :--- | :--- | :--- |
| **[ADR-001](#adr-001-layered-backend-architecture)** | Layered Backend Architecture | Accepted | 2026-06-18 |
| **[ADR-002](#adr-002-postgresql-as-primary-database)** | PostgreSQL as Primary Database | Accepted | 2026-06-18 |
| **[ADR-003](#adr-003-store-money-as-integer-minor-units)** | Store Money as Integer Minor Units (Paise) | Accepted | 2026-06-18 |
| **[ADR-004](#adr-004-ledger-based-balance-calculation)** | Ledger-Based Balance Calculation & Projections | Accepted | 2026-06-18 |
| **[ADR-005](#adr-005-flyway-database-migrations)** | Flyway Database Schema Evolution | Accepted | 2026-06-18 |
| **[ADR-006](#adr-006-greedy-debt-simplification-algorithm)** | Greedy Debt Simplification Algorithm | Accepted | 2026-06-19 |
| **[ADR-007](#adr-007-reversing-ledger-entries-over-hard-deletes)** | Reversing Ledger Entries over Physical Deletes | Accepted | 2026-06-20 |
| **[ADR-008](#adr-008-react-typescript-vite-frontend-architecture)** | React + TypeScript + Vite Frontend Architecture | Accepted | 2026-06-21 |
| **[ADR-009](#adr-009-redis-backed-token-bucket-rate-limiting)** | Redis-Backed Token Bucket Rate Limiting | Accepted | 2026-06-22 |
| **[ADR-010](#adr-010-centralized-documentation-hierarchy-in-docs)** | Centralized Documentation Hierarchy in `/docs` | Accepted | 2026-08-11 |
| **[ADR-011](#adr-011-structured-authentication-response-payload-authresponse)** | Structured Authentication Response Payload (`AuthResponse`) | Accepted | 2026-08-11 |
| **[ADR-012](#adr-012-client-authentication-context--protected-route-architecture)** | Client Authentication Context & Protected Route Architecture | Accepted | 2026-08-11 |
| **[ADR-013](#adr-013-fine-grained-security-endpoint-configuration--error-handling)** | Fine-Grained Security Endpoint Configuration & Error Handling | Accepted | 2026-08-11 |
| **[ADR-014](#adr-014-multi-payer-expense-engine--ledger-netting-algorithm)** | Multi-Payer Expense Engine & Ledger Netting Algorithm | Accepted | 2026-08-11 |
| **[ADR-015](#adr-015-in-place-expense-edits-via-reversing-ledger)** | In-Place Expense Edits via Reversing Ledger | Accepted | 2026-08-11 |
| **[ADR-016](#adr-016-frontend-split-strategy-ui--custom-allocation-controls)** | Frontend Split Strategy UI & Custom Allocation Controls | Accepted | 2026-08-11 |
| **[ADR-017](#adr-017-expense-categorization--group-spending-taxonomy)** | Expense Categorization & Group Spending Taxonomy | Accepted | 2026-08-11 |
| **[ADR-018](#adr-018-receipt-storage--static-resource-attachment-engine)** | Receipt Storage & Static Resource Attachment Engine | Accepted | 2026-08-11 |
| **[ADR-019](#adr-019-redis-refresh-token-rotation--session-revocation)** | Redis Refresh Token Rotation & Session Revocation | Accepted | 2026-08-11 |

---

## Architecture Decision Details

### ADR-001: Layered Backend Architecture

- **Status**: Accepted
- **Date**: 2026-06-18
- **Context**: SettleSense contains complex business rules regarding expense splitting, balance calculation, settlements, and membership rules.
- **Decision**: Use a traditional layered Spring Boot architecture (`Controller` → `Service` → `Repository` → `Database`).
- **Alternatives Considered**:
  - *Controller-heavy architecture*: Rejected due to code duplication, testing difficulty, and poor maintainability.
- **Trade-offs & Benefits**: Introduces more classes upfront, but guarantees clear separation of concerns, easy testing, and long-term maintainability.

---

### ADR-002: PostgreSQL as Primary Database

- **Status**: Accepted
- **Date**: 2026-06-18
- **Context**: Financial transactions require strict ACID guarantees, strong relational consistency, and complex join capabilities across users, groups, expenses, and ledgers.
- **Decision**: Use PostgreSQL 16 as the primary relational database.
- **Alternatives Considered**:
  - *MongoDB*: Rejected due to weak multi-table transactional guarantees and complex balance aggregation queries.
- **Trade-offs & Benefits**: Requires strict schema design upfront, but provides absolute data correctness and transactional integrity.

---

### ADR-003: Store Money as Integer Minor Units

- **Status**: Accepted
- **Date**: 2026-06-18
- **Context**: Financial calculations must prevent floating-point precision errors (e.g. `0.1 + 0.2 = 0.30000000000000004`).
- **Decision**: Store all monetary values as integer minor units (`Long` paise for INR, e.g. ₹100.50 stored as `10050`).
- **Alternatives Considered**:
  - *Double/Float*: Rejected due to rounding errors.
  - *BigDecimal*: Considered, but integer minor units are simpler to compare, store, and compute deterministically across Java and PostgreSQL.
- **Trade-offs & Benefits**: Requires UI formatting utilities, but eliminates precision errors entirely.

---

### ADR-004: Ledger-Based Balance Calculation

- **Status**: Accepted
- **Date**: 2026-06-18
- **Context**: Dynamically recalculating user balances from historical expenses on every API request would degrade performance as expense counts grow.
- **Decision**: Use a double-entry style `LedgerEntry` table with cached `BalanceProjection` records. Expenses post ledger entries; net balances are projected incrementally.
- **Alternatives Considered**:
  - *On-the-fly SQL aggregation*: Rejected due to $O(N)$ scaling degradation on read requests.
- **Trade-offs & Benefits**: Requires projection synchronization logic, but yields lightning-fast $O(1)$ balance reads and complete auditability.

---

### ADR-005: Flyway Database Migrations

- **Status**: Accepted
- **Date**: 2026-06-18
- **Context**: Database schema updates must be version-controlled, repeatable, and automated across environments.
- **Decision**: Enforce Flyway for all schema migrations (`V1__...`, `V2__...`). Hibernate `ddl-auto` is disabled in production.
- **Alternatives Considered**:
  - *Manual DDL scripts / Hibernate auto-update*: Rejected due to high risk of data corruption or environment drift.
- **Trade-offs & Benefits**: Requires manual SQL migration writing, but ensures zero deployment drift.

---

### ADR-006: Greedy Debt Simplification Algorithm

- **Status**: Accepted
- **Date**: 2026-06-19
- **Context**: Group expense splitting results in complex pairwise debts (A owes B, B owes C, C owes A).
- **Decision**: Implement a greedy min-max debt reduction algorithm that calculates net balances for all members, separates debtors/creditors, and greedily matches maximum debtor with maximum creditor.
- **Alternatives Considered**:
  - *Full graph flow network (Ford-Fulkerson)*: Considered, but greedy min-max yields optimal $N-1$ settlement paths for group expenses with $O(N \log N)$ complexity.
- **Trade-offs & Benefits**: Computationally lightweight and reduces user payment friction significantly.

---

### ADR-007: Reversing Ledger Entries over Hard Deletes

- **Status**: Accepted
- **Date**: 2026-06-20
- **Context**: Financial applications must preserve audit trails. Hard-deleting expenses or settlements corrupts balance history.
- **Decision**: Financial entities are immutable from a ledger perspective. Cancellations or edits issue **Compensating Reversing Ledger Entries** with inverted debit/credit amounts.
- **Alternatives Considered**:
  - *Physical SQL DELETE*: Rejected due to loss of auditability and broken explainability.
- **Trade-offs & Benefits**: Table size grows monotonically, but complete financial history is preserved forever.

---

### ADR-008: React + TypeScript + Vite Frontend Architecture

- **Status**: Accepted
- **Date**: 2026-06-21
- **Context**: The user interface needs to be fast, responsive, type-safe, and capable of real-time balance rendering.
- **Decision**: Build the web application as a Single Page Application (SPA) using React 18, TypeScript, and Vite.
- **Alternatives Considered**:
  - *Thymeleaf Server-Side Rendering*: Rejected due to slow interactive UI feedback for complex split creation.
- **Trade-offs & Benefits**: Requires client-side SPA routing and build steps, but delivers a state-of-the-art interactive user experience.

---

### ADR-009: Redis-Backed Token Bucket Rate Limiting

- **Status**: Accepted
- **Date**: 2026-06-22
- **Context**: Critical endpoints require protection against abuse and sudden spikes in traffic across multi-instance backend deployments.
- **Decision**: Use Redis with a token bucket algorithm implemented via atomic Lua scripts for rate limiting.
- **Alternatives Considered**:
  - *In-memory rate limiting (Bucket4j)*: Rejected because local state is not shared across multi-node server clusters.
- **Trade-offs & Benefits**: Adds Redis infrastructure dependency, but guarantees consistent rate enforcement across node instances.

---

### ADR-010: Centralized Documentation Hierarchy in `/docs`

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Markdown documentation was previously scattered across root (`Agents.md`, `architecture-decisions.md`, `CLAUDE.md`, `DECISIONS.md`, `INSTRUCTIONS.md`, `README.md`, `ROADMAP.md`), `backend/`, and `frontend/`.
- **Decision**: Move all project markdown documentation under `/docs`, creating structured subdirectories (`docs/backend/` and `docs/frontend/`) for component-specific guides and maintaining single sources of truth.
- **Alternatives Considered**:
  - *Leaving scattered markdown files in subfolders*: Rejected due to document fragmentation, stale duplicates, and poor discoverability.
- **Trade-offs & Benefits**: Ensures clean root repository directory structure and unified documentation tree for AI agents and human developers alike.

---

### ADR-011: Structured Authentication Response Payload (`AuthResponse`)

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: `POST /api/auth/login` previously returned a raw JWT token string, forcing frontend clients to issue separate API requests or decode JWT claims client-side to retrieve user profile metadata.
- **Decision**: Return a structured JSON response record `AuthResponse(String token, Long userId, String email, String displayName)` from the authentication endpoint.
- **Alternatives Considered**:
  - *Plain text token response*: Rejected because client apps require user profile details (ID, display name) immediately upon authentication to populate UI state.
- **Trade-offs & Benefits**: Exposes essential user context alongside JWT token in a single atomic response, eliminating extra API round-trips during SPA application boot.

---

### ADR-012: Client Authentication Context & Protected Route Architecture

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Frontend single page application requires state management for user authentication, token persistence in `localStorage`, automatic Bearer header inclusion, and client-side route protection.
- **Decision**: Implement `AuthContext` with `AuthProvider`, custom hook `useAuth()`, token storage synchronization in `client.ts`, and a `ProtectedRoute` wrapper component protecting dashboard routes with redirection to `/login` and `/register`.
- **Alternatives Considered**:
  - *Unprotected client routes*: Rejected because unauthenticated access leaks UI actions and breaks security.
- **Trade-offs & Benefits**: Provides clean client-side authentication management, seamless token persistence across browser refreshes, and instant UX feedback on session expiration.

---

### ADR-013: Fine-Grained Security Endpoint Configuration & Error Handling

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Spring Security filter chain needed explicit rules for public user registration (`POST /api/users`) and centralized HTTP 403 / HTTP 401 error response handling for access-denied exceptions.
- **Decision**: Configure `POST /api/users` as `permitAll()` alongside `/api/auth/**` in `SecurityConfig`, and map `AccessDeniedException` to `403 FORBIDDEN` in `ApiExceptionHandler`.
- **Alternatives Considered**:
  - *Default Spring Boot error page*: Rejected because client applications expect structured JSON `ErrorResponse` payloads.
- **Trade-offs & Benefits**: Guarantees unauthenticated users can register while strictly protecting all group, expense, and ledger endpoints.

---

### ADR-014: Multi-Payer Expense Engine & Ledger Netting Algorithm

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Single expenses can be co-funded by multiple group members simultaneously (e.g. User A paid ₹600, User B paid ₹400 for a ₹1000 bill), requiring accurate pairwise double-entry ledger calculation without balance distortion.
- **Decision**: Extend `PostExpenseCommand` & `PostExpenseRequest` with optional `payerInputsByUserId` map and implement a net-position matching algorithm in `LedgerService.entriesForMultiPayerExpense`. Calculates net credit/debt for each participant (`Net_u = Paid_u - Owed_u`) and deterministically pairs net debtors with net creditors.
- **Alternatives Considered**:
  - *Generating duplicate full debt entries per payer*: Rejected because it overinflated transaction volume and introduces circular balances.
- **Trade-offs & Benefits**: Computes exact minimal pairwise ledger entries for single and multi-payer bills while maintaining 100% backwards compatibility for single-payer invocations.

---

### ADR-015: In-Place Expense Edits via Reversing Ledger

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Modifying existing expense details (amount, description, date, payers, or splits) must maintain immutable financial audit history without deleting historical ledger records.
- **Decision**: Implement `PUT /api/expenses/{expenseId}` endpoint and `ExpenseWorkflowService.editExpense(UpdateExpenseCommand)`. When an expense is updated, compensating reversing ledger entries (`REVERSAL`) are created for original ledger entries, old splits are cleared, new splits are persisted, and new replacement ledger entries (`EXPENSE`) are posted before refreshing balance projections.
- **Alternatives Considered**:
  - *Physical update of existing ledger rows*: Rejected because mutating historical ledger rows destroys auditability and balance point-in-time reconstruction.
- **Trade-offs & Benefits**: Preserves complete audit trail of financial state before and after edits.

---

### ADR-016: Frontend Split Strategy UI & Custom Allocation Controls

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: The web frontend required dynamic controls to select split strategies (`EQUAL`, `EXACT`, `PERCENTAGE`, `SHARES`) and enter custom split values per group member.
- **Decision**: Update `DashboardPage.tsx` with a Split Strategy dropdown selector, dynamic input rows per active group member, and client-side conversion (e.g. major unit ₹ to minor paise for `EXACT`, ratio numbers for `PERCENTAGE` / `SHARES`).
- **Alternatives Considered**:
  - *Hardcoding equal splits only*: Rejected because advanced split calculations are core product requirements.
- **Trade-offs & Benefits**: Expands frontend form controls while seamlessly mapping to backend `SplitCalculator` strategy models.

---

### ADR-017: Expense Categorization & Group Spending Taxonomy

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Group expense analysis requires categorizing transactions into domains (`GENERAL`, `FOOD`, `TRAVEL`, `UTILITIES`, `RENT`, `ENTERTAINMENT`, `SHOPPING`).
- **Decision**: Add `category` column to `expense` table via Flyway migration `V4__add_expense_category_and_receipt.sql`, update `Expense` entity with `@Enumerated(EnumType.STRING)`, and include category dropdown and category breakdown metrics in the frontend UI.
- **Alternatives Considered**:
  - *Uncategorized expenses*: Rejected due to inability to analyze group spending trends by domain.
- **Trade-offs & Benefits**: Enables instant category breakdown aggregations across group spending.

---

### ADR-018: Receipt Storage & Static Resource Attachment Engine

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Users need to attach receipt images or PDF files to expense entries for proof of payment.
- **Decision**: Implement `POST /api/expenses/{expenseId}/receipt` accepting multipart file uploads, store files under `uploads/receipts/`, configure `WebMvcConfig` to serve `/uploads/**` static resources, and render file attachment triggers and receipt viewing links in `DashboardPage.tsx`.
- **Alternatives Considered**:
  - *Third-party cloud storage dependency*: Deferred to production deployment phase; local file system storage provides zero-dependency development.
- **Trade-offs & Benefits**: Delivers instant receipt attachment capability with zero external cloud dependencies during development.

---

### ADR-019: Redis Refresh Token Rotation & Session Revocation

- **Status**: Accepted
- **Date**: 2026-08-11
- **Context**: Security best practices require short-lived Access Tokens paired with sliding Refresh Tokens stored securely and revocable upon user logout.
- **Decision**: Create `RefreshTokenService` using Redis (`refresh_token:<token>` with 7-day TTL), implement `POST /api/auth/refresh` for sliding token rotation (invalidating used refresh token and issuing a new token pair), and `POST /api/auth/logout` for session invalidation.
- **Alternatives Considered**:
  - *Stateless un-revocable JWT refresh tokens*: Rejected due to security risks when tokens are compromised.
- **Trade-offs & Benefits**: Guarantees instant session revocation across all devices while providing seamless long-lived session renewal.

 invocations.




