# Frontend Development Guide

This document covers the React frontend for SettleSense.

---

## Overview

| Property | Value |
|----------|-------|
| Framework | React 18+ |
| Language | TypeScript 5.x |
| Build Tool | Vite 6.x |
| Routing | React Router 7.x |
| Styling | CSS |

---

## Prerequisites

- Node.js 18+
- npm 9+

Verify Node installation:
```powershell
node --version
npm --version
```

---

## Quick Start

### 1. Install Dependencies

```powershell
cd frontend
npm install
```

### 2. Run Development Server

```powershell
npm run dev
```

The app will start on `http://localhost:5173` with hot module replacement.

### 3. Verify Backend Connection

The frontend proxies API requests to the backend. Ensure the backend is running on `http://localhost:8080`.

Open browser to `http://localhost:5173` and:
1. Create a user
2. Create a group
3. Add members
4. Post an expense to see balances update

---

## Development Commands

### Development

```powershell
# Start dev server with hot reload
npm run dev

# Start with custom port
npm run dev -- --port 3000

# Start with debugging
npm run dev -- --debug
```

### Building

```powershell
# Build for production
npm run build

# Preview production build
npm run preview

# Preview on custom port
npm run preview -- --port 3000
```

### Linting & Type Checking

```powershell
# Run ESLint
npm run lint

# Run TypeScript type check
npx tsc --noEmit

# Run both
npm run build
```

---

## Project Structure

```
frontend/
├── src/
│   ├── main.tsx                    # Application entry point
│   ├── router.tsx                 # Route definitions
│   ├── styles.css                  # Global styles
│   │
│   ├── api/                        # API client layer
│   │   ├── client.ts              # Base fetch wrapper
│   │   ├── domain.ts              # Domain types & functions
│   │   └── system.ts              # System status
│   │
│   ├── auth/                       # Authentication
│   │   └── authState.ts           # Auth state management
│   │
│   └── ui/                         # UI Components
│       ├── AppLayout.tsx           # Main layout wrapper
│       ├── NotFoundPage.tsx       # 404 page
│       └── DashboardPage.tsx      # Main dashboard
│
├── index.html                     # HTML entry point
├── package.json                   # Dependencies
├── tsconfig.json                  # TypeScript config
├── tsconfig.app.json              # App TypeScript config
├── tsconfig.node.json             # Node TypeScript config
├── vite.config.ts                 # Vite configuration
└── eslint.config.js               # ESLint configuration
```

---

## Key Files

### `src/main.tsx`

Application entry point. Sets up React and renders the app.

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { router } from './router'
import './styles.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
)
```

### `src/router.tsx`

Defines application routes. Currently includes Dashboard and NotFound routes.

```tsx
import { createBrowserRouter } from 'react-router-dom'
import { DashboardPage } from './ui/DashboardPage'
import { NotFoundPage } from './ui/NotFoundPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <DashboardPage />,
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])
```

### `src/api/client.ts`

Base API client using fetch. Handles requests to the backend.

```typescript
async function apiRequest<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || `HTTP ${response.status}`)
  }

  return response.json()
}
```

### `src/api/domain.ts`

Domain types and API functions for interacting with the backend.

**Types:**
```typescript
type User = { id: number; displayName: string; email: string; status: string }
type Group = { id: number; name: string; currencyCode: string; status: string; createdByUserId: number }
type GroupMember = { id: number; groupId: number; userId: number; role: 'OWNER' | 'MEMBER'; status: 'ACTIVE' | 'LEFT' | 'REMOVED' }
type Balance = { fromUserId: number; toUserId: number; currencyCode: string; amountMinor: number }
type Expense = { id: number; groupId: number; paidByUserId: number; description: string; currencyCode: string; totalMinor: number; expenseDate: string; status: string }
type Settlement = { id: number; groupId: number; fromUserId: number; toUserId: number; currencyCode: string; amountMinor: number; settlementDate: string; status: string }
```

**Functions:**
```typescript
listUsers(): Promise<User[]>
registerUser(input: { displayName: string; email: string }): Promise<User>
listGroups(): Promise<Group[]>
createGroup(input: { name: string; currencyCode: string; createdByUserId: number }): Promise<Group>
listGroupMembers(groupId: number): Promise<GroupMember[]>
addGroupMember(groupId: number, input: { userId: number; actorUserId: number; role: 'MEMBER' }): Promise<GroupMember>
postExpense(groupId: number, input: PostExpenseInput): Promise<Expense>
postSettlement(groupId: number, input: PostSettlementInput): Promise<Settlement>
listBalances(groupId: number): Promise<Balance[]>
listSettlementSuggestions(groupId: number): Promise<Balance[]>
```

### `src/ui/DashboardPage.tsx`

Main application page with:
- User registration form
- Group creation and selection
- Member management
- Expense posting (equal split)
- Settlement recording
- Balance display
- Settlement suggestions display

---

## Configuration

### Vite Config (`vite.config.ts`)

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

The proxy configuration forwards `/api` requests to the Spring Boot backend at `http://localhost:8080`.

### TypeScript Config

The project uses strict TypeScript mode. Key settings in `tsconfig.json`:
- `strict: true`
- `noUnusedLocals: true`
- `noUnusedParameters: true`
- `noImplicitReturns: true`

---

## Adding New Features

### Adding a New Page

1. Create component in `src/ui/`:
   ```tsx
   // src/ui/MyPage.tsx
   export function MyPage() {
     return <div>My New Page</div>
   }
   ```

2. Add route in `src/router.tsx`:
   ```tsx
   import { MyPage } from './ui/MyPage'
   
   // Add to routes array:
   {
     path: '/my-page',
     element: <MyPage />,
   },
   ```

### Adding API Endpoint

1. Add type to `src/api/domain.ts`:
   ```typescript
   export type MyEntity = {
     id: number
     name: string
   }
   ```

2. Add API function:
   ```typescript
   export function getMyEntities() {
     return apiRequest<MyEntity[]>('/api/myentities')
   }
   ```

### Using API in Component

```tsx
import { useEffect, useState } from 'react'
import { listUsers, type User } from '../api/domain'

function MyComponent() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listUsers()
      .then(setUsers)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div>Loading...</div>

  return (
    <ul>
      {users.map(user => <li key={user.id}>{user.displayName}</li>)}
    </ul>
  )
}
```

---

## Styling

### Global Styles (`styles.css`)

The project uses CSS for styling. Main classes:

```css
/* Layout */
.workspace       /* Main container */
.work-grid       /* 2-column grid */
.panel           /* Card/container */

/* Forms */
.form-stack      /* Vertical form layout */
.form-row        /* Horizontal form layout */

/* Data Display */
.data-list       /* List items */
.button-list     /* Button groups */
.pill            /* Badge/label */

/* Status */
.status-strip    /* Top status bar */
```

### Adding Styles

```css
.my-component {
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 8px;
}

.my-component h2 {
  margin: 0 0 0.5rem;
  font-size: 1.25rem;
}
```

---

## API Client

### Request Format

```typescript
// GET request
apiRequest<User[]>('/api/users')

// POST request
apiRequest<User>('/api/users', {
  method: 'POST',
  body: JSON.stringify({ displayName: 'John', email: 'john@example.com' })
})
```

### Error Handling

Errors throw as `Error` objects with the response text or HTTP status message:

```typescript
try {
  await registerUser({ displayName: 'John', email: 'john@example.com' })
} catch (error) {
  if (error instanceof Error) {
    console.error(error.message)
  }
}
```

---

## Troubleshooting

### Port 5173 Already in Use

Change port in `vite.config.ts`:
```typescript
server: {
  port: 3000,
}
```

### CORS Errors

The frontend proxies `/api` to the backend. If seeing CORS errors:
1. Ensure backend is running on `http://localhost:8080`
2. Check the proxy config in `vite.config.ts`

### TypeScript Errors

```powershell
# Check for errors
npx tsc --noEmit

# Check specific file
npx tsc src/ui/DashboardPage.tsx --noEmit --skipLibCheck
```

### Build Errors

```powershell
# Clear cache and rebuild
rm -rf node_modules/.vite
npm run build
```

### ESLint Errors

```powershell
# Fix auto-fixable issues
npm run lint -- --fix

# Check specific file
npx eslint src/ui/DashboardPage.tsx
```

---

## Current Features

### Implemented

- ✅ User registration
- ✅ Group creation
- ✅ Group selection
- ✅ Member management (add members)
- ✅ Expense posting (EQUAL split)
- ✅ Settlement recording
- ✅ Balance display
- ✅ Settlement suggestions display

### Not Yet Implemented

- ❌ User login/authentication
- ❌ Login page
- ❌ User profile page
- ❌ Edit expense (only cancellation works)
- ❌ Edit settlement
- ❌ Split type selection (only EQUAL)
- ❌ Loading states
- ❌ Error handling UI
- ❌ Confirmation dialogs
- ❌ Responsive design

---

## Future Enhancements

### Authentication
- Login page
- JWT token storage
- Protected routes
- Logout functionality

### UI/UX
- Loading spinners
- Error toasts
- Confirmation dialogs
- Form validation feedback
- Responsive design
- Dark mode

### Features
- Expense editing
- Expense cancellation UI
- Settlement cancellation UI
- Extended split types (EXACT, PERCENTAGE, SHARE)
- Activity feed
- User profile management

### Technical
- Global state management (Context/Redux)
- API response caching
- Optimistic updates
- Unit tests
- Component tests
- E2E tests