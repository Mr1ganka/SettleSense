# SettleSense Backend Learning Roadmap

> **Last Updated:** 2026-06-21  
> **Status:** Phase 1 Complete âœ…

---

# How To Use This Roadmap

This roadmap is not only a list of features to implement.

It is also a structured backend engineering learning journey.

The goal is to build SettleSense while developing strong engineering skills required for production backend systems.

The agent should help me learn by:

- Explaining engineering concepts behind each feature
- Discussing design trade-offs before implementation
- Challenging my assumptions
- Asking questions before proposing solutions
- Helping me make engineering decisions
- Reviewing my implementation choices
- Connecting implementation decisions to real production systems

The objective is not only:

"Make the feature work"

The objective is:

"Understand why the feature is designed this way and how it would behave in a production system."

---

# Feature Implementation Learning Workflow

For every roadmap item, follow this workflow.

## 1. Understand The Problem

Before implementation discuss:

- Why does this feature exist?
- What user/business problem does it solve?
- What are the requirements?
- What edge cases exist?

---

## 2. Explore Current Design

Before changing code:

Inspect:

- Existing entities
- Services
- Controllers
- Repositories
- Database schema
- Existing patterns

Understand:

- What already exists?
- What can be extended?
- What should not be changed?

Avoid redesigning existing systems without justification.

---

## 3. Design Discussion

Before writing code, discuss:

### API Design

Consider:

- Endpoint design
- HTTP methods
- Request/response models
- Validation
- Error handling

### Database Design

Consider:

- Tables
- Relationships
- Constraints
- Indexes
- Data consistency

### Backend Design

Consider:

- Transactions
- Concurrency
- Security
- Performance
- Scalability

---

## 4. Implementation

Implementation should follow a pair-programming approach.

Prefer:

- Small changes
- Incremental commits
- Explaining decisions
- Reviewing code after implementation

Do not generate large amounts of code without discussion.

---

## 5. Engineering Reflection

After completing each feature, discuss:

### What Did We Learn?

Important concepts introduced.

### Why Was This Design Chosen?

Alternatives considered.

### Production Considerations

What changes would be needed at scale?

### Interview Perspective

What SDE-2/Senior engineer questions could be asked about this feature?

---

# Engineering Maturity Goals

By completing this roadmap, I should develop understanding of:

## Backend Fundamentals

- REST API design
- Database modeling
- Transactions
- Validation
- Error handling
- Testing

## Backend Architecture

- Layered architecture
- Domain modeling
- Service boundaries
- Event-driven design
- Async processing

## Production Engineering

- Security
- Observability
- Performance
- Scalability
- Reliability
- Deployment practices

## System Design

Ability to reason about:

- 100K users
- 1M users
- 10M users

Including:

- Database scaling
- Caching
- Queues
- Distributed systems
- Failure handling

---

# Roadmap Rules

When updating this roadmap:

Update feature status:

- Planned
- In Progress
- Completed
- Needs Investigation

Include:

- What was implemented
- Important design decisions
- Remaining limitations

Do not mark a feature complete only because code exists.

A feature is complete when:

- Functionality works
- Edge cases are handled
- Tests exist
- Design decisions are understood
- Production considerations are discussed

---

## Phase 1: Foundation & Core Features

*Status: âœ… COMPLETE*

### 1.1 Database Schema

**Status: âœ… Implemented**

- All core entities exist: `User`, `Group`, `GroupMember`, `Expense`, `ExpenseSplit`, `Settlement`, `Friendship`, `ActivityEvent`, `LedgerEntry`, `BalanceProjection`
- Enums: `SplitType`, `ExpenseStatus`, `SettlementStatus`, `GroupStatus`, `GroupMemberRole`, `FriendshipStatus`, etc.
- Uses PostgreSQL with Flyway migrations
- Money stored as `long` minor units (no floating point)

**Main Classes:**
- `backend/src/main/java/com/kelvin/settlesense/domain/model/*`

### 1.2 User Management

**Status: âœ… Implemented**

- `POST /api/users` - Register user
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user profile

**Main Classes:**
- `UserController.java`
- `UserWorkflowService.java`
- `UserRepository.java`

**Edge Cases Handled:**
- Email uniqueness enforced
- Duplicate email returns 400 error
- Empty/invalid inputs rejected

### 1.3 Group Management

**Status: âœ… Implemented**

- `POST /api/groups` - Create group
- `GET /api/groups` - List all groups
- `GET /api/groups/{id}` - Get group by ID
- `GET /api/groups/{id}/members` - List group members
- `POST /api/groups/{id}/members` - Add member
- `POST /api/groups/{id}/members/{userId}/leave` - Leave group
- `POST /api/groups/{id}/members/{userId}/remove` - Remove member
- `POST /api/groups/{id}/archive` - Archive group

**Main Classes:**
- `GroupController.java`
- `GroupWorkflowService.java`
- `GroupRepository.java`
- `GroupMemberRepository.java`

**Edge Cases Handled:**
- Creator automatically added as OWNER
- Cannot add same user twice
- Cannot add to archived group
- Only OWNER can remove members or archive

### 1.4 Expense Creation with Splits

**Status: âœ… Implemented**

- `POST /api/groups/{groupId}/expenses` - Create expense
- `GET /api/groups/{groupId}/expenses` - List expenses in group
- `POST /api/expenses/{id}/cancel` - Cancel expense
- All four split types: `EQUAL`, `EXACT`, `PERCENTAGE`, `SHARE`

**Main Classes:**
- `ExpenseController.java`
- `ExpenseWorkflowService.java`
- `SplitCalculator.java`
- `ExpenseRepository.java`, `ExpenseSplitRepository.java`

**How It Works:**
1. `SplitCalculator` computes amounts based on `splitType` and user inputs
2. `ExpenseWorkflowService.postExpense()` validates group/members, creates expense, creates splits
3. Ledger entries generated automatically via `LedgerService`
4. Balance projections refreshed automatically

**Edge Cases Handled:**
- Equal split: handles remainder distribution
- Exact split: validates sum equals total
- Percentage split: validates sum equals 100%
- Share split: validates total shares > 0
- Cancelled expenses excluded from balances

### 1.5 Settlement Creation

**Status: âœ… Implemented**

- `POST /api/groups/{groupId}/settlements` - Create settlement
- `GET /api/groups/{groupId}/settlements` - List settlements in group
- `POST /api/settlements/{id}/cancel` - Cancel settlement

**Main Classes:**
- `SettlementController.java`
- `SettlementWorkflowService.java`
- `SettlementRepository.java`

**Edge Cases Handled:**
- Both users must be active group members
- Settlement reverses ledger entries on cancel

### 1.6 Balance Calculation

**Status: âœ… Implemented**

- `GET /api/groups/{id}/balances` - Get all balances
- `GET /api/groups/{id}/settlement-suggestions` - Get simplified settlement suggestions

**Main Classes:**
- `BalanceController.java`
- `LedgerService.java`
- `BalanceProjectionService.java`
- `BalanceProjectionUpdater.java`
- `BalanceProjectionRepository.java`

**How It Works:**
1. Every expense/settlement creates `LedgerEntry` records
2. `BalanceProjectionUpdater.refresh()` recalculates all balances from ledger
3. `BalanceProjectionService.suggestSimplifiedSettlements()` uses greedy algorithm to minimize transactions

**Edge Cases Handled:**
- Handles zero balances (not included in projection)
- Cancelled expenses/settlements reverse ledger entries

### 1.7 Activity Logging

**Status: âœ… Implemented**

- Activity events created for `EXPENSE_POSTED`, `EXPENSE_CANCELLED`, `SETTLEMENT_POSTED`, `SETTLEMENT_CANCELLED`
- `ActivityEventFactory` creates events
- `ActivityEventRepository` stores them
- `GET /api/groups/{id}/activity` - Group activity feed API

**Main Classes:**
- `ActivityEvent.java` (entity)
- `ActivityEventFactory.java`
- `ActivityEventRepository.java`
- `ActivityController.java`

---

## Phase 2: Authentication & Authorization

*Status: ✅ COMPLETE*

### 2.1 User Login & JWT

**Status: âœ… COMPLETED**

**Completed:**
- âœ… User entity has passwordHash field (V3 migration)
- âœ… RegisterUserCommand accepts password
- âœ… PasswordConfig BCrypt encoder
- âœ… LoginUserDto, RegisterUserDto DTOs
- âœ… AuthController with login endpoint
- âœ… JwtService for token generation/validation
- âœ… JwtAuthenticationFilter for request authentication
- âœ… SecurityConfig with JWT filter chain
- âœ… Exception handler for 401 on bad credentials
- âœ… Unit tests for JwtService, AuthWorkflowService

**Not Completed:**


**Business Problem:**
Currently, the API has no authentication. Any user can call any endpoint. There's no way to know who is making a request.

**Why This Feature Exists:**
- Protect user data (only you can see your groups)
- Enable stateless authentication for mobile apps
- Track who created/modified resources

**Current Architecture:**
- `User` entity has no password field
- No JWT service exists
- `SecurityConfig` allows all `/api/**` endpoints without auth

**Affected Components:**

| Component | File | Changes Needed |
|-----------|------|----------------|
| Entity | `User.java` | Add `passwordHash`, `salt` fields |
| Service | New file | Add `JwtService` for token generation/validation |
| Controller | `UserController.java` | Add `POST /api/auth/login` |
| Config | `SecurityConfig.java` | Add JWT filter chain |
| DTOs | New file | Add `LoginRequest`, `LoginResponse` |

**Database Changes:**
```sql
-- Add to app_user table
ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE app_user ADD COLUMN salt VARCHAR(255);
```

**Design Questions:**
1. Should we store refresh tokens in the database or use JWT claims?
2. What's the token expiration strategy (short access + long refresh)?
3. Should we support OAuth (Google login) initially or stick to password-based?

**Implementation Tasks (30-60 min each):**
1. [x] Add password fields to User entity
2. [x] Create JWT service with generate/validate methods
3. [x] Add login endpoint
4. [x] Implement JWT security filter
5. [x] Update SecurityConfig with authentication rules
6. [x] Add password hashing (BCrypt)

**Acceptance Criteria:**
- [x] User can register with password (modify existing endpoint)
- [x] Login returns JWT token
- [x] Protected endpoints require valid JWT
- [x] Invalid credentials return 401

### 2.2 Resource Ownership & Authorization

**Status: ✅ COMPLETED**

**Business Problem:**
Authenticated requests now use the JWT principal as the actor, so client-supplied IDs are no longer trusted for ownership-sensitive actions.

**Completed:**
- Controller layer resolves the actor from the authenticated JWT principal when available
- Expense cancellation is restricted to the expense creator
- Settlement cancellation is restricted to the settlement creator
- Group membership removal and archive actions require active group ownership
- User profile updates are restricted to the authenticated user
- Request/response DTOs were moved into `domain/model/dto`
- Tests were updated for the authorization and DTO refactor

**Implementation Tasks:**
1. [x] Add user context to request flow
2. [x] Add ownership check to expense cancel
3. [x] Add ownership check to settlement cancel
4. [x] Add permission check to group operations

**Acceptance Criteria:**
- [x] User can only cancel their own expenses
- [x] User can only cancel settlements they created
- [x] Non-owner cannot remove members or archive group

### 2.3 Rate Limiting
**Status: ✅ Completed**

**Completed:**
- ✅ Redis-backed token bucket rate limiting
- ✅ Applied only to `GET /api/users` and `GET /api/groups`
- ✅ Keyed by authenticated user when available, otherwise client IP
- ✅ `429 Too Many Requests` response with `Retry-After`
- ✅ Unit tests for the limiter and filter wiring

**Design Decisions:**
- Redis chosen so the limiter works across multiple application instances
- Token bucket chosen over fixed-window counting to allow controlled bursts
- Implementation is fail-open if Redis is unavailable, so the API remains usable if the rate-limit store blips

**Remaining Limitations:**
- No per-endpoint policy differentiation yet
- No dashboard/metrics around rate-limit events yet
- Depends on Redis availability for enforcement

### 3.2 Filtering & Search

**Status: âŒ Not Implemented**

**Implementation Tasks:**
1. [ ] Add query parameters to expense list
2. [ ] Implement Specification-based queries
3. [ ] Add search parameter

### 3.3 OpenAPI Documentation

**Status: âŒ Not Implemented**

**Implementation Tasks:**
1. [ ] Add springdoc-openapi dependency
2. [ ] Add @Operation annotations to endpoints
3. [ ] Configure API info

---

## Phase 4: Social Features

*Status: ðŸŸ¡ PARTIALLY IMPLEMENTED*

### 4.1 Friendship System

**Status: ðŸŸ¡ Partially Implemented**

**Current State:**
- âœ… `FriendshipService.requestFriendship()` creates PENDING request
- âŒ No accept/reject endpoints
- âŒ No list friends endpoint

### 4.2 Group Invitations

**Status: âŒ Not Implemented**

**Implementation Tasks:**
1. [ ] Create GroupInvitation entity
2. [ ] Add generate invite endpoint
3. [ ] Add join with code endpoint

---

## Phase 5: Advanced Features

*Status: âŒ NOT IMPLEMENTED*

### 5.1 Expense Receipts/Attachments

**Status: âŒ Not Implemented**

### 5.2 Balance Explanation

**Status: âŒ Not Implemented**

**Implementation Tasks:**
1. [ ] Query all expenses between user pair
2. [ ] Subtract settlements
3. [ ] Return breakdown

### 5.3 Multi-Payer Expenses

**Status: âŒ Not Implemented**

### 5.4 Expense Editing

**Status: âŒ Not Implemented**

---

## Phase 6: Performance & Scalability

*Status: âŒ NOT IMPLEMENTED*

### 6.1 Caching

**Status: âŒ Not Implemented**

### 6.2 Database Indexing

**Status: âŒ Not Implemented**

---

## Phase 7: Production Readiness

*Status: âŒ NOT IMPLEMENTED*

### 7.1 Dockerfile

**Status: âŒ Not Implemented**

### 7.2 CI/CD Pipeline

**Status: âŒ Not Implemented**

### 7.3 Monitoring & Observability

**Status: âŒ Not Implemented**

---

# Current State Summary
### âœ… Implemented (Phase 1 Complete)

| Feature | Status |
|---------|--------|
| Database schema | âœ… Complete |
| User registration | âœ… Complete |
| User profile update | âœ… Complete |
| Group CRUD | âœ… Complete |
| Member management | âœ… Complete |
| Expense creation (all split types) | âœ… Complete |
| Expense list endpoint | âœ… Complete |
| Expense cancellation | âœ… Complete |
| Settlement creation | âœ… Complete |
| Settlement list endpoint | âœ… Complete |
| Settlement cancellation | âœ… Complete |
| Balance calculation | âœ… Complete |
| Settlement suggestions | âœ… Complete |
| Activity logging (internal) | âœ… Complete |
| Activity feed API | âœ… Complete |

### ðŸŸ¡ In Progress (Phase 2)

| Feature | Status |
|---------|--------|
| User password storage | âœ… Complete |
| Login DTOs | âœ… Complete |
| Password encoder | âœ… Complete |
| JWT service | âœ… Complete |
| Login endpoint | âœ… Complete |
| JWT security filter | âœ… Complete |
| Auth unit tests | âœ… Complete |
| Resource authorization | ✅ Complete |
| Rate limiting | ✅ Complete |

### ✅ Implemented / Complete

| Feature | Priority |
|---------|----------|
| Resource authorization | ✅ Complete |
| Rate limiting | ✅ Complete |
| Pagination | MEDIUM |
| Filtering/Search | MEDIUM |
| OpenAPI docs | MEDIUM |
| Group invitations | MEDIUM |
| Friendship accept/reject | MEDIUM |
| Expense editing | LOW |
| Multi-payer | LOW |
| Receipt attachments | LOW |
| Balance explanation | LOW |
| Caching | LOW |
| Docker | LOW |
| CI/CD | LOW |

---

## Recommended Implementation Order

### Recommended Learning Sequence

1. **JWT Authentication** - This is blocking many other features
2. **Resource Authorization** - Security vulnerability fixed

### Short-term (Next)

3. Expense list endpoint with pagination
4. OpenAPI documentation

### Medium-term (After that)

6. Group invitations
7. Friendship accept/reject
8. Expense editing
9. Filtering & search

### Long-term (then)

10. Multi-payer expenses
11. Receipt attachments
12. Balance explanation
13. Caching
14. Docker & CI/CD

---

## Learning Questions for Each Feature

When implementing each feature, consider:

### API Design
- What HTTP method? What status codes?
- Request/response DTOs?
- Query parameters vs path variables?

### Database Design
- New table or extend existing?
- Indexes needed?
- Foreign key constraints?

### Validation
- What inputs need validation?
- Error messages?

### Authorization
- Who can access this?
- Who can modify this?

### Error Handling
- What can go wrong?
- HTTP status codes?

### Scalability
- Will this scale to 1M users?
- N+1 queries?

### Concurrency
- What if two requests happen at once?
- Transaction boundaries?

