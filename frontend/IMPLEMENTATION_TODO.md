# Frontend - What Needs to Be Implemented

This document lists the features, enhancements, and improvements that are not yet implemented in the SettleSense frontend.

> **Status:** Phase 1 Complete ✅ (Last Updated: 2026-06-18)

---

## Authentication

### User Login
- ❌ **Login Page** - No dedicated login screen
- ❌ **Login Form** - No email/password form
- ❌ **Authentication State** - Not persisted
- ❌ **Login API** - No backend login endpoint to connect to
- ❌ **Token Storage** - No JWT token handling

### Auth Guards
- ❌ **Protected Routes** - No route protection
- ❌ **Redirect to Login** - No unauthorized redirect
- ❌ **Session Persistence** - No localStorage/sessionStorage

### Logout
- ❌ **Logout Button** - No logout UI
- ❌ **Logout Action** - No logout handler

---

## User Management

### Profile
- ❌ **Profile Page** - No user profile view
- ❌ **Edit Profile** - No name/email edit
- ❌ **Profile Settings** - No settings page

---

## Group Features

### Group Details
- ❌ **Group Detail Page** - No separate group view
- ❌ **Group Settings** - No edit group settings
- ❌ **Group Description** - No description field

### Member Management
- ❌ **Remove Member** - No UI to remove member
- ❌ **Leave Group** - No leave group button
- ❌ **Member Roles** - No role display/change

### Group Actions
- ❌ **Archive Group** - No archive button (backend exists)
- ❌ **Reopen Group** - No unarchive
- ❌ **Delete Group** - No delete option

---

## Expense Features

### Expense Creation
- ❌ **Split Type Selection** - Only EQUAL, no choice
- ❌ **EXACT Split UI** - Input exact amounts per person
- ❌ **PERCENTAGE Split UI** - Input percentages
- ❌ **SHARE Split UI** - Input share ratios
- ❌ **Multi-Payer** - No split payment UI

### Expense Management
- ✅ **Expense List** - Now shows expenses in group
- ❌ **Expense Details** - No detail view
- ❌ **Edit Expense** - No edit form
- ❌ **Cancel Expense Button** - Backend works, but needs UI
- ❌ **Expense Comments** - No comments

### Expense Display
- ❌ **Receipt Images** - No image display
- ❌ **Expense History** - No timeline view

---

## Settlement Features

### Settlement Management
- ✅ **Settlement List** - Now shows settlements in group
- ❌ **Settlement Details** - No detail view
- ❌ **Cancel Settlement UI** - Backend exists, needs UI

---

## Balance & Activity

### Balance Features
- ❌ **Balance Details** - No breakdown of balances
- ❌ **Balance Explanation** - No "why do I owe" detail
- ❌ **Balance History** - No balance over time

### Activity Feed
- ❌ **Activity List** - No activity feed display
- ❌ **Group Activity** - Backend exists, needs UI

---

## UI/UX Improvements

### Loading States
- ❌ **Loading Spinner** - No loading indicator
- ❌ **Skeleton Screens** - No skeleton UI
- ❌ **Loading Messages** - No "loading..." text

### Error Handling
- ❌ **Error Toasts** - No popup notifications
- ❌ **Error Boundaries** - No React error boundaries
- ❌ **Retry Buttons** - No retry on failure

### Forms
- ❌ **Validation Messages** - No inline errors
- ❌ **Required Field Indicators** - No asterisk
- ❌ **Form Reset** - Inconsistent reset
- ❌ **Disabled Submit During Submit** - No double-submit guard

### Confirmation
- ❌ **Delete Confirmation** - No confirm dialog
- ❌ **Cancel Confirmation** - No confirm dialog
- ❌ **Settlement Confirmation** - No confirm dialog

### Visual Feedback
- ❌ **Button Hover States** - Limited hover styles
- ❌ **Focus States** - No focus indicators
- ❌ **Success Animation** - No success feedback
- ❌ **Toast Notifications** - No popup messages

---

## Layout & Design

### Responsive Design
- ❌ **Mobile Layout** - Not optimized for mobile
- ❌ **Tablet Layout** - No tablet breakpoints
- ❌ **Desktop Layout** - Basic desktop only

### Navigation
- ❌ **Sidebar** - No navigation sidebar
- ❌ **Header** - No app header
- ❌ **Breadcrumbs** - No navigation trail
- ❌ **Tab Navigation** - No tabs

### Pages Missing
- ❌ **Dashboard Redesign** - Could be improved
- ❌ **Login Page**
- ❌ **Register Page**
- ❌ **Profile Page**
- ❌ **Group List Page**
- ❌ **Group Detail Page**
- ❌ **Expense History Page**
- ❌ **Settlement History Page**
- ❌ **Activity Feed Page**
- ❌ **Settings Page**

### Design System
- ❌ **Color Palette** - Limited colors
- ❌ **Typography** - Single font
- ❌ **Spacing Scale** - No consistent spacing
- ❌ **Shadows** - No elevation system

### Dark Mode
- ❌ **Dark Theme** - No dark mode
- ❌ **Theme Toggle** - No switcher

---

## State Management

### Global State
- ❌ **Context API** - No app-wide state
- ❌ **Redux** - No state management library

### Data Caching
- ❌ **React Query** - No data fetching library
- ❌ **SWR** - No stale-while-revalidate
- ❌ **Cache Invalidation** - No manual refresh control

### Optimistic Updates
- ❌ **Immediate Feedback** - No optimistic UI
- ❌ **Rollback on Error** - No error rollback

---

## Technical Improvements

### TypeScript
- ❌ **Strict Mode** - Could be stricter
- ❌ **Type Coverage** - Some any types

### Components
- ❌ **Component Library** - No shared components
- ❌ **Reusable Button** - Not abstracted
- ❌ **Reusable Input** - Not abstracted

### Testing
- ❌ **Unit Tests** - No Jest tests
- ❌ **Component Tests** - No testing-library
- ❌ **E2E Tests** - No Playwright/Cypress

### Build
- ❌ **Environment Variables** - No .env support
- ❌ **Bundle Analysis** - No bundle size analysis
- ❌ **Code Splitting** - No lazy loading

---

## Summary

| Category | Priority | Items |
|----------|----------|-------|
| Authentication | High | Login page, protected routes, token storage |
| UI/UX | High | Loading states, error handling, confirmations |
| Expense Features | High | Split types, edit/cancel UI |
| Responsive Design | Medium | Mobile, tablet layouts |
| State Management | Medium | Context, caching, optimistic updates |
| Pages | Medium | Login, profile, history pages |
| Testing | Low | Unit tests, e2e tests |

---

## Recommended Implementation Order

### Phase 2.1 (Immediate)
1. Login page and authentication flow
2. Protected routes and auth guards
3. Loading states and error toasts
4. Expense cancellation UI

### Phase 2.2 (Short-term)
5. Expense history page
6. Split type selection UI
7. Responsive design
8. Confirmation dialogs

### Phase 2.3 (Medium-term)
9. Profile page
10. Activity feed
11. Dark mode
12. Better form validation

### Phase 3 (Long-term)
13. Component library
14. Full test coverage
15. PWA features