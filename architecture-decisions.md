# SettleSense Architecture Decisions

This document records important architectural and technical decisions made during the development of SettleSense.

The purpose of this document is to capture:
- Why a decision was made
- What alternatives were considered
- What trade-offs were accepted
- What future engineers should understand before changing the design

This document should be updated when significant architectural decisions are made.

## Repository Documentation System

This repository uses multiple documents with different responsibilities.

### AGENTS.md

**Purpose:**  
Defines how coding agents should behave.

**Contains:**
- Development workflow
- Communication style
- Learning approach
- Decision-making process

### roadmap.md

**Purpose:**  
Defines what we are building.

**Contains:**
- Product features
- Implementation phases
- Current priorities
- Learning objectives

### instructions.md

**Purpose:**  
Defines how we build software.

**Contains:**
- Coding conventions
- Engineering practices
- Project standards
- Development rules

### architecture-decisions.md

**Purpose:**  
Records why technical decisions were made.

**Contains:**
- Architectural choices
- Design trade-offs
- Technology decisions
- Future considerations

## Architecture Decision Records

### ADR-001: Layered Backend Architecture

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
Use a layered Spring Boot architecture.

**Structure:**
- Controller
- Service
- Repository
- Database


**Responsibilities:**

#### Controllers
Responsible for:
- HTTP handling
- Request validation
- Response formatting

Should not contain business logic.

#### Services
Responsible for:
- Business workflows
- Domain operations
- Transaction boundaries
- Business validation

#### Repositories
Responsible for:
- Database access
- Entity persistence
- Queries

**Context:**  
SettleSense contains business rules around:
- Expense splitting
- Balance calculation
- Settlements
- Group membership
- Authorization

Keeping logic inside controllers would make the application difficult to maintain as complexity increases.

**Alternatives Considered**

| Option | Controller-heavy architecture |
|--------|------------------------------|
| Pros   | Faster initial development, Fewer classes |
| Cons   | Controllers become complex, Business logic becomes duplicated, Harder testing |
| Decision | **Rejected** |

| Option | Layered architecture |
|--------|---------------------|
| Pros   | Clear separation of responsibility, Easier testing, Familiar Spring Boot pattern, Better long-term maintainability |
| Cons   | More files and structure |
| Decision | **Accepted** |

**Trade-offs**

Accepted:
- Additional classes
- More upfront structure

Benefits:
- Maintainability
- Testability
- Easier future scaling

---

### ADR-002: PostgreSQL as Primary Database

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
Use PostgreSQL as the primary database.

**Context:**  
The application contains strongly related entities:
- Users
- Groups
- Group members
- Expenses
- Expense splits
- Settlements
- Ledger entries

Financial data requires:
- Strong consistency
- Transactions
- Data integrity

**Alternatives Considered**

| Option | MongoDB |
|--------|---------|
| Pros   | Flexible schema, Faster prototyping |
| Cons   | Complex relationships, More difficult transactional workflows, Harder reporting queries |
| Decision | **Rejected** |

| Option | PostgreSQL |
|--------|------------|
| Pros   | Strong relational model, Transactions, Constraints, Mature ecosystem |
| Cons   | - |
| Decision | **Accepted** |

**Trade-offs:**  
Relational databases require more upfront schema design.

**Benefits:**
- Data correctness
- Strong consistency
- Better financial integrity

---

### ADR-003: Store Money as Minor Units

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
Store money values as integer minor units.

**Example:**  
₹100.50 stored as: **10050 paise**

**Context:**  
Financial calculations must avoid floating point precision errors.

**Alternatives Considered**

| Option | Double / Float |
|--------|----------------|
| Decision | **Rejected** |
| Reason   | Floating point arithmetic can introduce rounding issues |

| Option | BigDecimal |
|--------|------------|
| Decision | Considered |
| Advantages | Exact decimal representation |
| Disadvantages | More verbose, Requires scale management |

**Decision Reasoning:**  
Integer minor units provide:
- Exact calculations
- Predictable storage
- Simple comparisons

---

### ADR-004: Ledger-Based Balance Calculation

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
Use a ledger-based approach for balance calculations.

**Flow:**
- Expense
- Ledger Entries
- Balance Projection


**Context:**  
Expenses and settlements create financial relationships.

Calculating balances directly from all historical transactions on every request would become expensive as data grows.

**Alternatives Considered**

| Option | Calculate balances from expenses every request |
|--------|------------------------------------------------|
| Pros   | Simple implementation |
| Cons   | Expensive queries, Poor scalability, Difficult optimization |
| Decision | **Rejected** |

| Option | Ledger + Projection |
|--------|---------------------|
| Pros   | Fast reads, Audit history, Better scaling path |
| Cons   | Additional complexity, Projection maintenance required |
| Decision | **Accepted** |

**Trade-offs:**

Introduces:
- Additional tables
- Synchronization logic

Provides:
- Faster queries
- Better auditability
- Clear financial history

---

### ADR-005: Flyway Database Migrations

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
All database schema changes must use Flyway migrations.

**Migration format:**
- V1__description.sql
- V2__description.sql
- V3__description.sql


**Context:**  
Database changes need to be:
- Version controlled
- Repeatable
- Deployable across environments

**Alternatives Considered**

| Option | Manual database changes |
|--------|-------------------------|
| Decision | **Rejected** |
| Problems | Difficult reproduction, Environment differences, Deployment risk |

| Option | Hibernate auto schema generation |
|--------|----------------------------------|
| Decision | **Rejected for production usage** |
| Problems | Less control, Risky with existing data |

**Decision:**  
Use Flyway as the source of truth for database evolution.

---

### ADR-006: Authentication Strategy

| Attribute | Value |
|-----------|-------|
| Status    | Planned |

**Decision:**  
Authentication strategy will be decided during Phase 2 implementation.

**Context:**  
Current system does not have authentication.

**Future requirements:**
- User identity
- Protected APIs
- Ownership validation
- Mobile application support

**Questions To Resolve:**
- JWT only or JWT with refresh tokens?
- Store refresh tokens or keep stateless?
- OAuth support?
- Token expiration strategy?
- Session invalidation approach?

---

### ADR-007: Authorization Strategy

| Attribute | Value |
|-----------|-------|
| Status    | Planned |

**Context:**  
Future versions require authorization rules.

**Examples:**
- Only group owners can archive groups
- Only authorized users can modify expenses
- Users should only access their own resources

**Questions To Resolve:**
- Role-based authorization or permission-based authorization?
- Where should authorization checks live?
- How should ownership rules be modeled?

---

### ADR-008: Learning-Oriented Development Process

| Attribute | Value |
|-----------|-------|
| Status    | Accepted |
| Date      | 2026-06-18 |

**Decision:**  
Development should optimize for both:
- Building a production-quality application
- Developing strong backend engineering judgment

**Context:**  
The project is intentionally used as a learning environment.

The goal is not only feature completion.  
The goal is understanding:
- Architecture
- Trade-offs
- Scalability
- Production concerns

**Development Principles**

**Before implementation:**
- Understand existing design
- Discuss requirements
- Evaluate alternatives

**During implementation:**
- Prefer small changes
- Explain decisions
- Avoid unnecessary complexity

**After implementation:**
- Review production concerns
- Discuss scalability
- Document important decisions

---

## Future Architecture Decisions

Create new ADRs when introducing:
- New infrastructure
- New architectural patterns
- Database design changes
- Security approaches
- Scaling strategies
- Major technology choices

**Examples:**
- Redis caching strategy
- Event-driven architecture
- Kafka adoption
- Search infrastructure
- Deployment architecture
- Monitoring and observability
- Cloud infrastructure
- CI/CD pipeline