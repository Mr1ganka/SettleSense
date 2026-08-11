# SettleSense - Engineering Instructions & Coding Standards

This document defines the engineering conventions, development practices, architectural rules, and coding standards for the SettleSense codebase.

---

## 1. Core Engineering Principles

1. **Production-Quality First**: Write code that is clean, self-documenting, maintainable, and production-ready.
2. **Strict Financial Precision**: All monetary values are integer minor units (e.g., paise in INR). Floating-point data types (`float`, `double`) are strictly prohibited in financial paths.
3. **Immutability & Explainability**: Financial events (expenses, settlements) are append-only. Cancellations or edits generate compensating reversing entries in the `LedgerEntry` table.
4. **Layered Decoupling**: Maintain clear boundaries: `Controller` (HTTP & Validation) → `Service` (Business Workflows & Transactions) → `Repository` (Persistence & Queries) → `Database`.
5. **Database as Single Source of Schema Truth**: Schema evolution is strictly managed via Flyway versioned SQL scripts. JPA entity definitions must match Flyway schemas.

---

## 2. Directory Layout & Module Responsibilities

```
SettleSense/
├── docs/                      # Technical documentation, flows, ADRs, & roadmap
├── backend/                   # Spring Boot 3.x backend application
│   ├── src/main/java/com/kelvin/settlesense/
│   │   ├── api/               # REST Controllers, DTO request/response records
│   │   ├── domain/            # JPA Entities, Embeddables, and Enums
│   │   ├── repository/        # Spring Data JPA Repositories
│   │   ├── service/           # Workflow Services & Money/Split Engines
│   │   ├── exception/         # Custom Domain Exception definitions & Global Handler
│   │   └── config/            # Spring Configuration Beans & Security
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/      # Flyway SQL schema scripts (V1__, V2__...)
├── frontend/                  # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── components/        # Presentational & Interactive React components
│   │   ├── pages/             # Page views & routing containers
│   │   ├── services/          # API HTTP client & service methods
│   │   ├── types/             # TypeScript interfaces & domain types
│   │   └── utils/             # Money formatting, date helpers, validators
└── docker/                    # Infrastructure services (PostgreSQL 16, Redis)
```

---

## 3. Backend Conventions (Java 17 & Spring Boot)

### Layer Responsibilities

- **Controllers (`com.kelvin.settlesense.api`)**:
  - Accept DTO requests, validate inputs (`@Valid`, `@NotNull`), delegate to Service layer.
  - Return standardized DTO responses (`ResponseEntity<T>`).
  - No database queries or business calculations in controllers.

- **Services (`com.kelvin.settlesense.service`)**:
  - Encapsulate business logic, split calculations, transaction boundaries (`@Transactional`).
  - Perform domain validation (e.g., membership check, currency consistency check).
  - Throw domain-specific exceptions (e.g., `ExpenseNotFoundException`, `InvalidSplitException`).

- **Repositories (`com.kelvin.settlesense.repository`)**:
  - Standard Spring Data JPA interfaces.
  - Custom JPQL/SQL queries should be explicit and covered by repository tests.

- **Entities (`com.kelvin.settlesense.domain`)**:
  - Primary keys must use `UUID` or `Long` (surrogate ID).
  - Include audit timestamps (`createdAt`, `updatedAt`).
  - Use `Long` for minor monetary amounts (e.g. `Long totalMinor`).

### Code Style Rules

- **Java Version**: Use Java 17 features where appropriate (Records for DTOs, Pattern Matching, Switch Expressions).
- **Naming**: `CamelCase` for classes, `camelCase` for methods/variables, `UPPER_SNAKE_CASE` for constants and enum values.
- **DTOs**: Standardize request and response DTOs using Java Records (`public record CreateExpenseRequest(...)`).
- **Null Safety**: Validate method arguments early. Avoid returning `null`; return `Optional<T>` for query lookups.

---

## 4. Frontend Conventions (React + TypeScript + Vite)

- **Language**: Strict TypeScript (`tsconfig.json` with strict mode enabled).
- **Styling**: Modern Vanilla CSS / Tailwind with responsive grid/flexbox layouts.
- **Component Design**: Functional components with React Hooks (`useState`, `useEffect`, `useCallback`, `useMemo`).
- **State Management**: Keep local transient UI state in local component hooks; lift shared state to parent context or custom hooks.
- **API Requests**: Isolated service layer under `src/services/` using `axios` or native `fetch` with error boundary handling.
- **Money Display**: Never do raw mathematical operations on floats in the UI. Formatter utility `formatMinorToCurrency(minorAmount: number, currencyCode: string)` converts `10050` paise → `₹100.50`.

---

## 5. Database & Migration Guidelines

- **Flyway Strictness**: Direct database DDL modifications are forbidden. Every schema change requires a new migration script in `backend/src/main/resources/db/migration/` (e.g., `V3__add_multi_payer_support.sql`).
- **Naming Rules**: SQL tables and columns must use `snake_case` (e.g., `expense_splits`, `total_minor`, `currency_code`).
- **Foreign Keys & Indexes**: Add explicit foreign key constraints and index foreign keys / query columns (e.g. `idx_group_member_user_group`).

---

## 6. Financial Calculations & Money Rules

1. **Paise Storage**: All money is represented in paise (1 INR = 100 Paise).
2. **Split Allocations**:
   - `EQUAL`: $\text{share} = \lfloor \frac{\text{total}}{N} \rfloor$. Remainder paise are allocated deterministically to the payer or first member so $\sum \text{splits} = \text{total}$.
   - `EXACT`: $\sum \text{exact_amounts} == \text{total}$.
   - `PERCENTAGE`: $\sum \text{percentages} == 100.00\%$. Round split paise to nearest integer. Handle rounding delta on final share.
   - `SHARES`: Each member gets $(\text{total} \times \text{member_shares}) / \text{total_shares}$.
3. **Reversal Entries**: Never use `DELETE FROM ledger_entry`. To cancel an expense, issue matching credit/debit entries with inverted amounts and reference `cancellation_of_expense_id`.

---

## 7. Testing & Quality Assurance

- **Backend Unit & Integration Tests**:
  - JUnit 5 + AssertJ + Mockito.
  - Repository and Service tests for financial logic (`SplitCalculatorTests`, `LedgerAndBalanceServiceTests`).
  - Full Spring Boot integration tests for Controllers (`PhaseOneControllerTests`).
- **Running Tests**:
  ```powershell
  cd backend
  .\gradlew.bat test
  ```
- **Frontend Verification**:
  ```powershell
  cd frontend
  npm run lint
  npm run build
  ```

---

## 8. AI Agent & Collaboration Workflow

When working with an AI assistant on this repository:
1. **Understand Requirements**: Review `docs/FLOW.md` and `docs/ROADMAP.md` before making architectural changes.
2. **Incremental Development**: Implement features in small, testable increments.
3. **Mandatory Verification**: Never report a task as complete without executing tests (`.\gradlew.bat test` or `npm run build`) to verify zero regressions.
4. **Update Documentation**: Keep `docs/DECISIONS.md` updated when making architectural or technical choices.
