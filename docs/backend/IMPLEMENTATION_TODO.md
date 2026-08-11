# Backend - What Needs to Be Implemented

This document lists the features, enhancements, and improvements that are not yet implemented in the SettleSense backend.

> **Status:** Phase 2 In Progress 🟡 (Last Updated: 2026-06-19)

---

## Authentication & Security

### User Authentication
- 🟡 **In Progress** - User entity has passwordHash field
- ✅ **RegisterUserCommand** - Now accepts password
- ✅ **PasswordConfig** - BCrypt encoder configured
- ❌ **No Login Endpoint** - AuthController shell exists but login not implemented
- ❌ **No JWT Service** - No token generation/validation
- ❌ **No OAuth Integration** - No Google, Apple, or other OAuth providers
- ❌ **No Session Management** - No session handling

### Authorization
- ❌ **No Role-Based Access** - No granular permissions
- ❌ **No Resource Ownership Checks** - Users can modify any resource
- ❌ **No Group Member Verification** - No check if user is member before expense

### Security Enhancements
- ❌ **CSRF Protection** - Currently disabled for dev
- ❌ **Rate Limiting** - No API rate limiting
- ❌ **Input Sanitization** - No XSS protection

---

## Expense Features

### Expense Creation
- ❌ **Multi-Payer Expenses** - Only single payer supported
- ❌ **Expense Editing** - Only cancellation, no in-place edit
- ❌ **Backdated Expenses** - No support for historical dates
- ❌ **Recurring Expenses** - No recurring expense feature

### Expense Attachments
- ❌ **Receipt Upload** - No file upload for receipts
- ❌ **Receipt Storage** - No attachment handling

---

## Friendship System

The Friendship entity exists but has limited functionality:

- ❌ **Friend Accept/Reject** - No accept/reject flow
- ❌ **Friend List** - No list friends endpoint
- ❌ **Friend Blocking** - BLOCKED status exists but unused

---

## Activity & Insights

### Activity Events
- ✅ **Activity Creation** - Entity exists and fully wired
- ✅ **Activity Query API** - GET /api/groups/{id}/activity

### Insight Requests
- ❌ **Balance Explanation API** - No "why do I owe X" endpoint

---

## API Enhancements

### Pagination
- ❌ **User List Pagination** - Returns all users
- ❌ **Group List Pagination** - Returns all groups
- ✅ **Expense List Endpoint** - GET /api/groups/{id}/expenses implemented

### Filtering & Querying
- ❌ **Date Range Filters** - No date filtering
- ❌ **Status Filters** - No status filtering
- ❌ **Search** - No search functionality
- ❌ **Sorting** - No sorting parameters

### API Design
- ❌ **API Versioning** - No versioned endpoints
- ❌ **OpenAPI/Swagger** - No API documentation

---

## Data Management

### User Management
- ✅ **User Profile Update** - PUT /api/users/{id}

### Group Management
- ❌ **Group Update** - No update name endpoint
- ❌ **Group Reopening** - Can't unarchive

---

## Validation & Error Handling

### Validation
- ❌ **Custom Business Rules** - Minimal validation
- ❌ **Cross-Entity Validation** - No complex validation

### Error Handling
- ❌ **Detailed Error Responses** - Basic exception messages
- ❌ **Error Codes** - No error code system

---

## Performance & Scalability

### Database
- ❌ **Connection Pooling** - Default HikariCP (may need tuning)
- ❌ **Indexes** - No custom indexes defined

### Caching
- ❌ **Response Caching** - No caching layer
- ❌ **Balance Caching** - Projections could be cached
- ❌ **Redis** - No Redis integration

### Async Processing
- ❌ **Async Operations** - All operations synchronous

---

## Testing Enhancements

### Unit Tests
- ❌ **Controller Unit Tests** - Limited @WebMvcTest tests
- ❌ **Service Unit Tests** - Missing for some services

### Integration Tests
- ❌ **Full E2E Tests** - Limited workflow tests
- ❌ **Authenticated Tests** - No auth in tests

---

## Documentation

- ❌ **API Documentation** - No Swagger/OpenAPI
- ❌ **Code Documentation** - Minimal Javadoc

---

## Deployment & Operations

### Configuration
- ❌ **Environment-Specific Config** - No profiles for prod
- ❌ **Secrets Management** - No vault integration

### DevOps
- ❌ **Dockerfile** - No container image
- ❌ **Kubernetes** - No K8s manifests
- ❌ **CI/CD Pipeline** - No build pipeline
- ❌ **Health Checks** - Only basic actuator

### Monitoring
- ❌ **Metrics** - No Micrometer metrics
- ❌ **Tracing** - No distributed tracing

---

## Summary

| Category | Priority | Items |
|----------|----------|-------|
| Authentication | High | Login, JWT, OAuth |
| Expense Features | High | Multi-payer, editing |
| API Enhancements | Medium | Pagination, filtering, Swagger |
| Friendship | Medium | Friend requests, discovery |
| Performance | Low | Caching, async, Redis |
| Testing | Medium | More unit tests, E2E |
| Documentation | Low | API docs, Javadoc |
| DevOps | Low | Dockerfile, CI/CD |

---

## Recommended Implementation Order

### Phase 2.1 (Immediate)
1. User authentication with JWT
2. Login endpoint
3. Resource ownership verification

### Phase 2.2 (Short-term)
4. Expense editing
5. Extended split types
6. Multi-payer expenses
7. Pagination on list endpoints

### Phase 2.3 (Medium-term)
8. OpenAPI documentation
9. Friendship system
10. Better error handling

### Phase 3 (Long-term)
11. Performance optimization
12. Caching layer
13. Docker deployment
14. CI/CD pipeline
## Phase 2 Update - Rate Limiting Completed (2026-06-22)

- ✅ **Rate Limiting** moved to IMPLEMENTATION_DONE.md
- ✅ Redis-backed limiter, filter wiring, Docker service, and tests are now implemented

Remaining security work should now focus on future enhancements such as input sanitization, observability, and any additional auth hardening that comes up later.
