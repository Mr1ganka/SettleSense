# Backend - What Has Been Implemented

This document lists all the features, components, and functionality that have been implemented in the SettleSense backend.

> **Status:** Phase 2 In Progress 🟡 (Last Updated: 2026-06-19)

---

## Core Infrastructure

### Application Setup
- ✅ **Spring Boot 3.x Application** - Main entry point (`SettleSenseApplication.java`)
- ✅ **Gradle Build System** - With wrapper for cross-platform builds
- ✅ **Java 17+ Support** - Modern Java features
- ✅ **Spring Security** - Basic security configuration (CSRF disabled for dev)

### Database
- ✅ **PostgreSQL 16** - Database driver and configuration
- ✅ **Flyway Migrations** - Database schema versioning
  - `V1__create_app_metadata.sql` - Initial schema
  - `V2__create_phase_1_domain_model.sql` - Phase 1 entities
- ✅ **Hibernate/JPA** - ORM for database operations

### Configuration
- ✅ **application.properties** - Main configuration
- ✅ **application-test.properties** - Test configuration with H2
- ✅ **SecurityConfig.java** - HTTP security setup
- ✅ **TimeConfig.java** - Timezone configuration

---

## Domain Model (22 Entities)

### User & Membership
- ✅ **User** - Person who can participate in groups and expenses
- ✅ **UserStatus** - ACTIVE, DEACTIVATED enum
- ✅ **Group** - Shared context (trip, household, event)
- ✅ **GroupStatus** - ACTIVE, ARCHIVED enum
- ✅ **GroupMember** - User membership in a group
- ✅ **GroupMemberRole** - OWNER, MEMBER enum
- ✅ **GroupMemberStatus** - ACTIVE, LEFT, REMOVED enum

### Financial Entities
- ✅ **Expense** - Bill or cost paid by a user
- ✅ **ExpenseStatus** - POSTED, CANCELLED enum
- ✅ **ExpenseSplit** - How an expense is allocated
- ✅ **SplitType** - EQUAL, EXACT, PERCENTAGE, SHARE enum
- ✅ **Settlement** - Actual payment between users
- ✅ **SettlementStatus** - POSTED, CANCELLED enum
- ✅ **LedgerEntry** - Normalized money movement (source of truth)
- ✅ **LedgerDirection** - OWES, PAID enum
- ✅ **LedgerSourceType** - EXPENSE, SETTLEMENT, REVERSAL enum

### Supporting Entities
- ✅ **BalanceProjection** - Derived outstanding balances
- ✅ **Friendship** - User relationships outside groups
- ✅ **FriendshipStatus** - PENDING, ACCEPTED, BLOCKED, CANCELLED enum
- ✅ **ActivityEvent** - Human-readable audit trail
- ✅ **InsightRequest** - Request for analysis/explanation

---

## Data Access (11 Repositories)

- ✅ **UserRepository** - User CRUD and queries
- ✅ **GroupRepository** - Group CRUD and queries
- ✅ **GroupMemberRepository** - Member management with custom queries
- ✅ **ExpenseRepository** - Expense queries
- ✅ **ExpenseSplitRepository** - Split queries
- ✅ **SettlementRepository** - Settlement queries
- ✅ **LedgerEntryRepository** - Ledger queries
- ✅ **BalanceProjectionRepository** - Balance queries
- ✅ **FriendshipRepository** - Friendship queries
- ✅ **ActivityEventRepository** - Activity queries
- ✅ **InsightRequestRepository** - Insight queries

---

## Business Services (17 Services)

### Workflow Services
- ✅ **UserWorkflowService** - User registration
- ✅ **GroupWorkflowService** - Group CRUD, member management, archiving
- ✅ **ExpenseWorkflowService** - Expense posting, cancellation, ledger creation
- ✅ **SettlementWorkflowService** - Settlement recording, cancellation

### Calculation Services
- ✅ **SplitCalculator** - Calculate equal, exact, percentage, share splits with rounding
- ✅ **BalanceProjectionService** - Calculate balances from ledger entries
- ✅ **BalanceProjectionUpdater** - Update balance projections

### Supporting Services
- ✅ **LedgerService** - Ledger entry management
- ✅ **FriendshipService** - Basic friendship management
- ✅ **ActivityEventFactory** - Create activity events
- ✅ **MoneyRules** - Money validation rules

### Command Objects
- ✅ **RegisterUserCommand** - User registration input
- ✅ **CreateGroupCommand** - Group creation input
- ✅ **AddGroupMemberCommand** - Member addition input
- ✅ **PostExpenseCommand** - Expense posting input
- ✅ **PostSettlementCommand** - Settlement input

### Data Objects
- ✅ **CalculatedSplit** - Split calculation result
- ✅ **SimplifiedSettlement** - Settlement suggestion

---

## REST API Controllers (6 Controllers)

### UserController
- ✅ `POST /api/users` - Register new user
- ✅ `GET /api/users` - List all users
- ✅ `GET /api/users/{id}` - Get user by ID
- ✅ `PUT /api/users/{id}` - Update user profile

### GroupController
- ✅ `POST /api/groups` - Create group
- ✅ `GET /api/groups` - List all groups
- ✅ `GET /api/groups/{id}` - Get group by ID
- ✅ `GET /api/groups/{id}/members` - List group members
- ✅ `POST /api/groups/{id}/members` - Add member
- ✅ `POST /api/groups/{id}/members/{userId}/leave` - Leave group
- ✅ `POST /api/groups/{id}/members/{userId}/remove` - Remove member
- ✅ `POST /api/groups/{id}/archive` - Archive group

### ExpenseController
- ✅ `POST /api/groups/{id}/expenses` - Post expense
- ✅ `GET /api/groups/{id}/expenses` - List expenses in group
- ✅ `POST /api/expenses/{id}/cancel` - Cancel expense

### SettlementController
- ✅ `POST /api/groups/{id}/settlements` - Record settlement
- ✅ `GET /api/groups/{id}/settlements` - List settlements in group
- ✅ `POST /api/settlements/{id}/cancel` - Cancel settlement (inherited via base)

### BalanceController
- ✅ `GET /api/groups/{id}/balances` - Get balances
- ✅ `GET /api/groups/{id}/settlement-suggestions` - Get simplified settlements

### SystemController
- ✅ `GET /api/health` - Health check

### ActivityController
- ✅ `GET /api/groups/{id}/activity` - Get group activity feed

### Exception Handling
- ✅ **ApiExceptionHandler** - Global exception handler

---

## Business Logic Implementation

### Money Handling
- ✅ Money stored as integer minor units (paise for INR)
- ✅ Currency code tracking
- ✅ Rounding with deterministic remainder distribution

### Expense Posting
- ✅ Single payer expenses
- ✅ Equal split calculation
- ✅ Ledger entry creation for each split
- ✅ Payer's own split does not create debt
- ✅ Balance projection updates

### Settlement Recording
- ✅ Settlement creates PAID ledger entry
- ✅ Balances update correctly
- ✅ Over-settlement reverses balance direction

### Expense Cancellation
- ✅ Cancellation creates REVERSAL ledger entries
- ✅ Original expense marked as CANCELLED
- ✅ Balance projections update

### Settlement Cancellation
- ✅ Cancellation creates REVERSAL ledger entries
- ✅ Original settlement marked as CANCELLED
- ✅ Balance projections update

### Balance Calculation
- ✅ Derives balances from ledger entries
- ✅ Pairwise netting
- ✅ Rebuildable from ledger

### Settlement Suggestions
- ✅ Simplified settlement algorithm
- ✅ Net position calculation
- ✅ Matches debtors to creditors optimally

---

## Testing

### Integration Tests
- ✅ **PhaseOneControllerTests** - Controller-level API tests
  - User registration tests
  - Group creation tests
  - Member management tests

### Service Tests
- ✅ **PhaseOneWorkflowIntegrationTests** - End-to-end workflow tests
- ✅ **LedgerAndBalanceServiceTests** - Ledger and balance calculation tests
- ✅ **SplitCalculatorTests** - Split calculation unit tests

---

## Summary

| Category | Count |
|----------|-------|
| Entities | 22 |
| Repositories | 11 |
| Services | 17 |
| Controllers | 6 |
| API Endpoints | ~25 |
| Test Classes | 4 |

The backend implements a complete Phase 1 expense management system with proper ledger-based accounting, balance calculation, and settlement handling.

---

## Phase 2: Authentication & Authorization (In Progress)

### Database
- ✅ **V3 Migration** - Added password_hash column to app_user table

### Entity Updates
- ✅ **User.passwordHash** - Field added to store BCrypt hashed password

### Configuration
- ✅ **PasswordConfig** - BCryptPasswordEncoder bean configured (strength 12)

### DTOs
- ✅ **RegisterUserCommand** - Updated to accept password field
- ✅ **LoginUserCommand** - DTO for login (email, password)
- ✅ **AuthResponse** - DTO for auth response (jwt token)

### Controllers
- ✅ **AuthController** - Shell controller at `/api/auth`

### Services
- ✅ **AuthWorkflowService** - Shell service for auth logic