import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  addGroupMember,
  createGroup,
  listBalances,
  listGroupMembers,
  listGroups,
  listSettlementSuggestions,
  listUsers,
  postExpense,
  postSettlement,
  registerUser,
  type Balance,
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
      const [nextMembers, nextBalances, nextSuggestions] = await Promise.all([
        listGroupMembers(nextGroupId),
        listBalances(nextGroupId),
        listSettlementSuggestions(nextGroupId),
      ]);
      setMembers(nextMembers);
      setBalances(nextBalances);
      setSuggestions(nextSuggestions);
    } else {
      setMembers([]);
      setBalances([]);
      setSuggestions([]);
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

  async function handlePostExpense(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedGroupId) return;
    const form = new FormData(event.currentTarget);
    const paidByUserId = Number(form.get('paidByUserId'));
    const splitInputsByUserId = Object.fromEntries(activeMembers.map((member) => [String(member.userId), 1]));
    await runAction(
      () =>
        postExpense(selectedGroupId, {
          paidByUserId,
          description: String(form.get('description')),
          totalMinor: toMinorUnits(String(form.get('amount'))),
          expenseDate: String(form.get('expenseDate')),
          createdByUserId: paidByUserId,
          splitType: 'EQUAL',
          splitInputsByUserId,
        }),
      'Expense posted',
    );
    event.currentTarget.reset();
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
              <h3>Equal Expense</h3>
              <form className="form-stack" onSubmit={handlePostExpense}>
                <input name="description" placeholder="Description" required />
                <div className="form-row">
                  <input min="0.01" name="amount" placeholder="Amount" step="0.01" type="number" required />
                  <input name="expenseDate" type="date" defaultValue={today} required />
                </div>
                <select name="paidByUserId" required>
                  <option value="">Paid by</option>
                  {memberUsers.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.displayName}
                    </option>
                  ))}
                </select>
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
    </section>
  );
}
