import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  addGroupMember,
  createGroup,
  listBalances,
  listExpenses,
  listGroupMembers,
  listGroups,
  listSettlementSuggestions,
  listUsers,
  postExpense,
  postSettlement,
  registerUser,
  uploadReceipt,
  type Balance,
  type Expense,
  type ExpenseCategory,
  type Group,
  type GroupMember,
  type User,
} from '../api/domain';

import { getSystemStatus, type SystemStatus } from '../api/system';

const today = new Date().toISOString().slice(0, 10);

function toMinorUnits(value: string) {
  return Math.round(Number(value || '0') * 100);
}

function formatMoney(amountMinor: number, currencyCode: string) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: currencyCode,
  }).format(amountMinor / 100);
}

export function DashboardPage() {
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [balances, setBalances] = useState<Balance[]>([]);
  const [suggestions, setSuggestions] = useState<Balance[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
  const [notice, setNotice] = useState<string>('Ready');
  const [error, setError] = useState<string | null>(null);

  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<ExpenseCategory>('GENERAL');

  const selectedGroup = groups.find((group) => group.id === selectedGroupId) ?? null;
  const activeMembers = members.filter((member) => member.status === 'ACTIVE');
  const memberUsers = activeMembers
    .map((member) => users.find((user) => user.id === member.userId))
    .filter((user): user is User => Boolean(user));
  const owner = activeMembers.find((member) => member.role === 'OWNER') ?? null;

  const usersById = useMemo(() => new Map(users.map((user) => [user.id, user])), [users]);

  const refreshAll = useCallback(async (groupId: number | null) => {
    const [nextUsers, nextGroups] = await Promise.all([listUsers(), listGroups()]);
    setUsers(nextUsers);
    setGroups(nextGroups);

    const nextGroupId = groupId ?? nextGroups[0]?.id ?? null;
    setSelectedGroupId(nextGroupId);
    if (nextGroupId) {
      const [nextMembers, nextBalances, nextSuggestions, nextExpenses] = await Promise.all([
        listGroupMembers(nextGroupId),
        listBalances(nextGroupId),
        listSettlementSuggestions(nextGroupId),
        listExpenses(nextGroupId),
      ]);
      setMembers(nextMembers);
      setBalances(nextBalances);
      setSuggestions(nextSuggestions);
      setExpenses(nextExpenses);
    } else {
      setMembers([]);
      setBalances([]);
      setSuggestions([]);
      setExpenses([]);
    }
  }, []);


  useEffect(() => {
    Promise.all([getSystemStatus(), refreshAll(null)])
      .then(([status]) => setSystemStatus(status))
      .catch((caught: unknown) => setError(caught instanceof Error ? caught.message : 'Unable to load app data'));
  }, [refreshAll]);

  async function runAction(action: () => Promise<unknown>, success: string) {
    setError(null);
    try {
      await action();
      await refreshAll(selectedGroupId);
      setNotice(success);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Action failed');
    }
  }

  function userName(userId: number) {
    return usersById.get(userId)?.displayName ?? `User ${userId}`;
  }

  async function handleRegisterUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(
      () =>
        registerUser({
          displayName: String(form.get('displayName')),
          email: String(form.get('email')),
        }),
      'User created',
    );
    event.currentTarget.reset();
  }

  async function handleCreateGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const ownerId = Number(form.get('createdByUserId'));
    setError(null);
    try {
      const group = await createGroup({
        name: String(form.get('name')),
        currencyCode: String(form.get('currencyCode') || 'INR'),
        createdByUserId: ownerId,
      });
      await refreshAll(group.id);
      setNotice('Group created');
      event.currentTarget.reset();
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Action failed');
    }
  }

  async function handleSelectGroup(groupId: number) {
    setError(null);
    try {
      await refreshAll(groupId);
      setNotice('Group loaded');
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Unable to load group');
    }
  }

  async function handleAddMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGroupId || !owner) return;
    const form = new FormData(event.currentTarget);
    await runAction(
      () =>
        addGroupMember(selectedGroupId, {
          userId: Number(form.get('userId')),
          actorUserId: owner.userId,
          role: 'MEMBER',
        }),
      'Member added',
    );
  }

  const [splitType, setSplitType] = useState<'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'SHARES'>('EQUAL');
  const [customSplitValues, setCustomSplitValues] = useState<Record<number, string>>({});

  async function handlePostExpense(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGroupId) return;
    const form = new FormData(event.currentTarget);
    const paidByUserId = Number(form.get('paidByUserId'));

    let splitInputsByUserId: Record<string, number> = {};
    if (splitType === 'EQUAL') {
      splitInputsByUserId = Object.fromEntries(activeMembers.map((member) => [String(member.userId), 1]));
    } else if (splitType === 'EXACT') {
      splitInputsByUserId = Object.fromEntries(
        activeMembers.map((m) => [String(m.userId), toMinorUnits(customSplitValues[m.userId] || '0')]),
      );
    } else if (splitType === 'PERCENTAGE' || splitType === 'SHARES') {
      splitInputsByUserId = Object.fromEntries(
        activeMembers.map((m) => [String(m.userId), Number(customSplitValues[m.userId] || '0')]),
      );
    }

    await runAction(
      () =>
        postExpense(selectedGroupId, {
          paidByUserId,
          description: String(form.get('description')),
          category: selectedCategory,
          totalMinor: toMinorUnits(String(form.get('amount'))),
          expenseDate: String(form.get('expenseDate')),
          createdByUserId: paidByUserId,
          splitType,
          splitInputsByUserId,
        }),
      'Expense posted',
    );
    event.currentTarget.reset();
    setCustomSplitValues({});
  }

  async function handleFileUpload(expenseId: number, file: File) {
    await runAction(
      () => uploadReceipt(expenseId, file),
      'Receipt attached',
    );
  }



  async function handlePostSettlement(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGroupId) return;
    const form = new FormData(event.currentTarget);
    const fromUserId = Number(form.get('fromUserId'));
    await runAction(
      () =>
        postSettlement(selectedGroupId, {
          fromUserId,
          toUserId: Number(form.get('toUserId')),
          amountMinor: toMinorUnits(String(form.get('amount'))),
          settlementDate: String(form.get('settlementDate')),
          createdByUserId: fromUserId,
        }),
      'Settlement recorded',
    );
    event.currentTarget.reset();
  }

  return (
    <section className="workspace">
      <div className="status-strip">
        <span>API: {systemStatus?.status ?? 'Checking'}</span>
        <span>{notice}</span>
        {error ? <strong>{error}</strong> : null}
      </div>

      <div className="work-grid">
        <section className="panel">
          <h2>People</h2>
          <form className="form-stack" onSubmit={handleRegisterUser}>
            <input name="displayName" placeholder="Display name" required />
            <input name="email" placeholder="Email" type="email" required />
            <button type="submit">Create User</button>
          </form>
          <ul className="data-list">
            {users.map((user) => (
              <li key={user.id}>
                <span>{user.displayName}</span>
                <small>{user.email}</small>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          <h2>Groups</h2>
          <form className="form-stack" onSubmit={handleCreateGroup}>
            <input name="name" placeholder="Group name" required />
            <div className="form-row">
              <input name="currencyCode" placeholder="INR" maxLength={3} defaultValue="INR" required />
              <select name="createdByUserId" required>
                <option value="">Owner</option>
                {users.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.displayName}
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" disabled={users.length === 0}>
              Create Group
            </button>
          </form>
          <div className="button-list">
            {groups.map((group) => (
              <button
                className={group.id === selectedGroupId ? 'selected' : ''}
                key={group.id}
                onClick={() => void handleSelectGroup(group.id)}
                type="button"
              >
                {group.name}
              </button>
            ))}
          </div>
        </section>
      </div>

      <section className="panel group-panel">
        <div className="section-head">
          <div>
            <p className="eyebrow">Active Group</p>
            <h2>{selectedGroup?.name ?? 'Create or select a group'}</h2>
          </div>
          {selectedGroup ? <span className="pill">{selectedGroup.currencyCode}</span> : null}
        </div>

        {selectedGroup ? (
          <div className="group-grid">
            <div>
              <h3>Members</h3>
              <form className="form-row" onSubmit={handleAddMember}>
                <select name="userId" required>
                  <option value="">Add person</option>
                  {users
                    .filter((user) => !members.some((member) => member.userId === user.id))
                    .map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.displayName}
                      </option>
                    ))}
                </select>
                <button type="submit" disabled={!owner}>
                  Add
                </button>
              </form>
              <ul className="data-list compact">
                {activeMembers.map((member) => (
                  <li key={member.id}>
                    <span>{userName(member.userId)}</span>
                    <small>{member.role}</small>
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3>Post Expense</h3>
              <form className="form-stack" onSubmit={handlePostExpense}>
                <input name="description" placeholder="Description" required />
                <div className="form-row">
                  <input min="0.01" name="amount" placeholder="Amount" step="0.01" type="number" required />
                  <input name="expenseDate" type="date" defaultValue={today} required />
                </div>
                <div className="form-row">
                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value as ExpenseCategory)}
                  >
                    <option value="GENERAL">General</option>
                    <option value="FOOD">Food & Dining</option>
                    <option value="TRAVEL">Travel & Transit</option>
                    <option value="UTILITIES">Utilities & Bills</option>
                    <option value="RENT">Rent & Housing</option>
                    <option value="ENTERTAINMENT">Entertainment</option>
                    <option value="SHOPPING">Shopping</option>
                  </select>
                  <select name="paidByUserId" required>
                    <option value="">Paid by</option>
                    {memberUsers.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.displayName}
                      </option>
                    ))}
                  </select>
                  <select
                    value={splitType}
                    onChange={(e) => setSplitType(e.target.value as any)}
                    id="split-type-select"
                  >
                    <option value="EQUAL">Split Equally</option>
                    <option value="EXACT">Split by Exact Amounts</option>
                    <option value="PERCENTAGE">Split by Percentages</option>
                    <option value="SHARES">Split by Share Ratios</option>
                  </select>
                </div>


                {splitType !== 'EQUAL' && (
                  <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <small style={{ fontWeight: 700, color: '#2c3e50' }}>
                      {splitType === 'EXACT' && 'Enter exact amount per person:'}
                      {splitType === 'PERCENTAGE' && 'Enter percentage per person (must total 100%):'}
                      {splitType === 'SHARES' && 'Enter share count per person:'}
                    </small>
                    {memberUsers.map((user) => (
                      <div key={user.id} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: '0.86rem', flex: 1 }}>{user.displayName}:</span>
                        <input
                          style={{ width: '120px', minHeight: '32px' }}
                          type="number"
                          step={splitType === 'EXACT' ? '0.01' : '1'}
                          placeholder={splitType === 'EXACT' ? '0.00' : splitType === 'PERCENTAGE' ? '33.3' : '1'}
                          value={customSplitValues[user.id] || ''}
                          onChange={(e) =>
                            setCustomSplitValues({ ...customSplitValues, [user.id]: e.target.value })
                          }
                        />
                      </div>
                    ))}
                  </div>
                )}

                <button type="submit" disabled={activeMembers.length < 2}>
                  Post Expense
                </button>
              </form>
            </div>


            <div>
              <h3>Settlement</h3>
              <form className="form-stack" onSubmit={handlePostSettlement}>
                <div className="form-row">
                  <select name="fromUserId" required>
                    <option value="">From</option>
                    {memberUsers.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.displayName}
                      </option>
                    ))}
                  </select>
                  <select name="toUserId" required>
                    <option value="">To</option>
                    {memberUsers.map((user) => (
                      <option key={user.id} value={user.id}>
                        {user.displayName}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-row">
                  <input min="0.01" name="amount" placeholder="Amount" step="0.01" type="number" required />
                  <input name="settlementDate" type="date" defaultValue={today} required />
                </div>
                <button type="submit" disabled={activeMembers.length < 2}>
                  Record Settlement
                </button>
              </form>
            </div>
          </div>
        ) : null}
      </section>

      <div className="work-grid">
        <section className="panel">
          <h2>Balances</h2>
          <ul className="data-list">
            {balances.length === 0 ? <li><span>No outstanding balances</span></li> : null}
            {balances.map((balance) => (
              <li key={`${balance.fromUserId}-${balance.toUserId}`}>
                <span>
                  {userName(balance.fromUserId)} owes {userName(balance.toUserId)}
                </span>
                <strong>{formatMoney(balance.amountMinor, balance.currencyCode)}</strong>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          <h2>Suggestions</h2>
          <ul className="data-list">
            {suggestions.length === 0 ? <li><span>No settlement needed</span></li> : null}
            {suggestions.map((suggestion) => (
              <li key={`${suggestion.fromUserId}-${suggestion.toUserId}`}>
                <span>
                  {userName(suggestion.fromUserId)} pays {userName(suggestion.toUserId)}
                </span>
                <strong>{formatMoney(suggestion.amountMinor, suggestion.currencyCode)}</strong>
              </li>
            ))}
          </ul>
        </section>
      </div>


      <div className="work-grid" style={{ marginTop: '20px' }}>
        <section className="panel">
          <h2>Expenses & Receipts</h2>

          <ul className="data-list">
            {expenses.length === 0 ? <li><span>No expenses posted yet</span></li> : null}
            {expenses.map((expense) => (
              <li key={expense.id} style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '6px' }}>
                <div style={{ display: 'flex', width: '100%', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong>{expense.description}</strong>
                    <span className="pill" style={{ marginLeft: '8px', fontSize: '0.72rem', background: '#e2e8f0', color: '#334155' }}>
                      {expense.category}
                    </span>
                  </div>
                  <strong>{formatMoney(expense.totalMinor, expense.currencyCode)}</strong>
                </div>
                <div style={{ display: 'flex', width: '100%', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.82rem', color: '#64748b' }}>
                  <span>Paid by {userName(expense.paidByUserId)} on {expense.expenseDate}</span>
                  {expense.receiptUrl ? (
                    <a href={expense.receiptUrl} target="_blank" rel="noreferrer" style={{ color: '#2563eb', fontWeight: 600 }}>
                      View Receipt 📎
                    </a>
                  ) : (
                    <label style={{ cursor: 'pointer', color: '#2563eb', fontWeight: 600 }}>
                      + Attach Receipt
                      <input
                        type="file"
                        accept="image/*,.pdf"
                        style={{ display: 'none' }}
                        onChange={(e) => {
                          if (e.target.files?.[0]) {
                            void handleFileUpload(expense.id, e.target.files[0]);
                          }
                        }}
                      />
                    </label>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          <h2>Category Breakdown</h2>
          <ul className="data-list">
            {expenses.length === 0 ? <li><span>No category data available</span></li> : null}
            {Object.entries(
              expenses.reduce<Record<string, number>>((acc, exp) => {
                const cat = exp.category || 'GENERAL';
                acc[cat] = (acc[cat] || 0) + exp.totalMinor;
                return acc;
              }, {}),
            ).map(([cat, totalMinor]) => (
              <li key={cat}>
                <span>{cat}</span>
                <strong>{formatMoney(totalMinor, selectedGroup?.currencyCode || 'INR')}</strong>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </section>
  );
}

