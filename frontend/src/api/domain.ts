import { apiRequest } from './client';

export type User = {
  id: number;
  displayName: string;
  email: string;
  status: string;
};

export type Group = {
  id: number;
  name: string;
  currencyCode: string;
  status: string;
  createdByUserId: number;
};

export type GroupMember = {
  id: number;
  groupId: number;
  userId: number;
  role: 'OWNER' | 'MEMBER';
  status: 'ACTIVE' | 'LEFT' | 'REMOVED';
};

export type Balance = {
  fromUserId: number;
  toUserId: number;
  currencyCode: string;
  amountMinor: number;
};

export type ExpenseCategory = 'GENERAL' | 'FOOD' | 'TRAVEL' | 'UTILITIES' | 'RENT' | 'ENTERTAINMENT' | 'SHOPPING';

export type Expense = {
  id: number;
  groupId: number;
  paidByUserId: number;
  description: string;
  currencyCode: string;
  totalMinor: number;
  expenseDate: string;
  status: string;
  category: ExpenseCategory;
  receiptUrl?: string;
};

export type Settlement = {
  id: number;
  groupId: number;
  fromUserId: number;
  toUserId: number;
  currencyCode: string;
  amountMinor: number;
  settlementDate: string;
  status: string;
};

export function listUsers() {
  return apiRequest<User[]>('/api/users');
}

export function registerUser(input: { displayName: string; email: string }) {
  return apiRequest<User>('/api/users', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listGroups() {
  return apiRequest<Group[]>('/api/groups');
}

export function createGroup(input: { name: string; currencyCode: string; createdByUserId: number }) {
  return apiRequest<Group>('/api/groups', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listGroupMembers(groupId: number) {
  return apiRequest<GroupMember[]>(`/api/groups/${groupId}/members`);
}

export function addGroupMember(groupId: number, input: { userId: number; actorUserId: number; role: 'MEMBER' }) {
  return apiRequest<GroupMember>(`/api/groups/${groupId}/members`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listExpenses(groupId: number) {
  return apiRequest<Expense[]>(`/api/groups/${groupId}/expenses`);
}

export function postExpense(
  groupId: number,
  input: {
    paidByUserId: number;
    description: string;
    category?: ExpenseCategory;
    totalMinor: number;
    expenseDate: string;
    createdByUserId: number;
    splitType: 'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'SHARES';
    splitInputsByUserId: Record<string, number>;
  },
) {
  return apiRequest<Expense>(`/api/groups/${groupId}/expenses`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function uploadReceipt(expenseId: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<Expense>(`/api/expenses/${expenseId}/receipt`, {
    method: 'POST',
    body: formData,
  });
}

export function postSettlement(
  groupId: number,
  input: {
    fromUserId: number;
    toUserId: number;
    amountMinor: number;
    settlementDate: string;
    createdByUserId: number;
  },
) {
  return apiRequest<Settlement>(`/api/groups/${groupId}/settlements`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listBalances(groupId: number) {
  return apiRequest<Balance[]>(`/api/groups/${groupId}/balances`);
}

export function listSettlementSuggestions(groupId: number) {
  return apiRequest<Balance[]>(`/api/groups/${groupId}/settlement-suggestions`);
}

