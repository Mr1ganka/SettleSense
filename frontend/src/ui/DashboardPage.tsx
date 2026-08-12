import React, { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import {
  User,
  Group,
  FriendshipResponse,
  Expense,
  Balance,
  ActivityEvent,
  GroupMember,
  listFriends,
  listFriendRequests,
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  listGroups,
  createGroup,
  addGroupMember,
  listGroupMembers,
  listExpenses,
  postExpense,
  postSettlement,
  listBalances,
  listActivity,
  ExpenseCategory
} from '../api/domain';

type TabType = 'dashboard' | 'friends' | 'groups';

export const DashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');

  // Core Data
  const [friends, setFriends] = useState<User[]>([]);
  const [friendRequests, setFriendRequests] = useState<FriendshipResponse[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [, setActivities] = useState<ActivityEvent[]>([]);

  // Selected Group State
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [groupMembers, setGroupMembers] = useState<GroupMember[]>([]);
  const [groupExpenses, setGroupExpenses] = useState<Expense[]>([]);
  const [groupBalances, setGroupBalances] = useState<Balance[]>([]);

  // Feedback notifications
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modals
  const [showAddFriendModal, setShowAddFriendModal] = useState<boolean>(false);
  const [friendEmailInput, setFriendEmailInput] = useState<string>('');

  const [showCreateGroupModal, setShowCreateGroupModal] = useState<boolean>(false);
  const [newGroupName, setNewGroupName] = useState<string>('');
  const [newGroupCurrency, setNewGroupCurrency] = useState<string>('INR');
  const [selectedFriendIdsForGroup, setSelectedFriendIdsForGroup] = useState<number[]>([]);

  // Add Expense Modal State
  const [showAddExpenseModal, setShowAddExpenseModal] = useState<boolean>(false);
  const [expenseDesc, setExpenseDesc] = useState<string>('');
  const [expenseAmount, setExpenseAmount] = useState<string>('');
  const [expenseCategory, setExpenseCategory] = useState<ExpenseCategory>('GENERAL');
  const [expensePaidByUserId, setExpensePaidByUserId] = useState<number>(user?.id || 0);

  // Settle Up Modal State
  const [showSettleModal, setShowSettleModal] = useState<boolean>(false);
  const [settleToUserId, setSettleToUserId] = useState<number>(0);
  const [settleAmountInput, setSettleAmountInput] = useState<string>('');

  // Currency Formatting Helper
  const getCurrencySymbol = (currencyCode: string = 'INR') => {
    switch (currencyCode.toUpperCase()) {
      case 'INR': return '₹';
      case 'EUR': return '€';
      case 'GBP': return '£';
      case 'USD': return '$';
      default: return '₹';
    }
  };

  const formatMoney = (amountMinor: number, currencyCode: string = 'INR') => {
    const symbol = getCurrencySymbol(currencyCode);
    return `${symbol}${(amountMinor / 100).toFixed(2)}`;
  };

  // Notification helpers
  const notifySuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(null), 4000);
  };

  const notifyError = (msg: string) => {
    setErrorMsg(msg);
    setTimeout(() => setErrorMsg(null), 5000);
  };

  // Main Data Refresh
  const refreshAllData = async (isBackground: boolean = false) => {
    try {
      const [friendsList, requestsList, groupsList, activityList] = await Promise.all([
        listFriends().catch(() => []),
        listFriendRequests().catch(() => []),
        listGroups().catch(() => []),
        listActivity().catch(() => [])
      ]);

      setFriends(friendsList);
      setFriendRequests(requestsList);
      setGroups(groupsList);
      setActivities(activityList);

      const targetGroup = selectedGroup || (groupsList.length > 0 ? groupsList[0] : null);
      if (targetGroup) {
        if (!selectedGroup) setSelectedGroup(targetGroup);
        await loadGroupData(targetGroup.id);
      }
    } catch (err: any) {
      if (!isBackground) setErrorMsg(err.message || 'Failed to sync data.');
    }
  };

  // Load specific group details
  const loadGroupData = async (groupId: number) => {
    try {
      const [members, exps, bals] = await Promise.all([
        listGroupMembers(groupId).catch(() => []),
        listExpenses(groupId).catch(() => []),
        listBalances(groupId).catch(() => [])
      ]);
      setGroupMembers(members);
      setGroupExpenses(exps);
      setGroupBalances(bals);
    } catch (err: any) {
      console.error('Failed to load group details', err);
    }
  };

  useEffect(() => {
    refreshAllData(false);

    // Background auto-sync every 3 seconds for real-time updates
    const intervalId = setInterval(() => {
      if (!document.hidden) {
        refreshAllData(true);
      }
    }, 3000);

    const handleFocus = () => refreshAllData(true);
    window.addEventListener('focus', handleFocus);

    return () => {
      clearInterval(intervalId);
      window.removeEventListener('focus', handleFocus);
    };
  }, [selectedGroup]);

  // Select Group & Open Group Workspace
  const handleSelectGroup = async (group: Group) => {
    setSelectedGroup(group);
    setActiveTab('groups');
    await loadGroupData(group.id);
  };

  // Add Friend Request
  const handleSendFriendRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!friendEmailInput.trim()) return;
    try {
      await sendFriendRequest({ email: friendEmailInput.trim() });
      notifySuccess(`Friend request sent to ${friendEmailInput}!`);
      setFriendEmailInput('');
      setShowAddFriendModal(false);
      refreshAllData(false);
    } catch (err: any) {
      notifyError(err.message || 'Could not send friend request.');
    }
  };

  const handleAcceptRequest = async (reqId: number) => {
    try {
      await acceptFriendRequest(reqId);
      notifySuccess('Friend request accepted!');
      refreshAllData(false);
    } catch (err: any) {
      notifyError(err.message || 'Could not accept request.');
    }
  };

  const handleRejectRequest = async (reqId: number) => {
    try {
      await rejectFriendRequest(reqId);
      notifySuccess('Friend request declined.');
      refreshAllData(false);
    } catch (err: any) {
      notifyError(err.message || 'Could not decline request.');
    }
  };

  // Create Group
  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim() || !user) return;
    try {
      const group = await createGroup({
        name: newGroupName.trim(),
        currencyCode: newGroupCurrency,
        createdByUserId: user.id
      });

      for (const friendId of selectedFriendIdsForGroup) {
        await addGroupMember(group.id, {
          userId: friendId,
          actorUserId: user.id,
          role: 'MEMBER'
        }).catch(() => {});
      }

      notifySuccess(`Group '${group.name}' created!`);
      setNewGroupName('');
      setSelectedFriendIdsForGroup([]);
      setShowCreateGroupModal(false);
      setSelectedGroup(group);
      setActiveTab('groups');
      refreshAllData(false);
    } catch (err: any) {
      notifyError(err.message || 'Failed to create group.');
    }
  };

  // Add Expense Handler (Modal Form)
  const handleModalAddExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !selectedGroup || !expenseDesc.trim() || !expenseAmount) return;
    try {
      const amountMinor = Math.round(parseFloat(expenseAmount) * 100);
      const members = groupMembers.length > 0 ? groupMembers : await listGroupMembers(selectedGroup.id);
      const memberCount = members.length || 1;
      const splitAmount = Math.floor(amountMinor / memberCount);

      const splitInputs: Record<string, number> = {};
      members.forEach(m => {
        splitInputs[m.userId.toString()] = splitAmount;
      });

      await postExpense(selectedGroup.id, {
        paidByUserId: expensePaidByUserId || user.id,
        description: expenseDesc.trim(),
        category: expenseCategory,
        totalMinor: amountMinor,
        expenseDate: new Date().toISOString(),
        createdByUserId: user.id,
        splitType: 'EQUAL',
        splitInputsByUserId: splitInputs
      });

      notifySuccess(`Expense '${expenseDesc.trim()}' added!`);
      setExpenseDesc('');
      setExpenseAmount('');
      setShowAddExpenseModal(false);
      loadGroupData(selectedGroup.id);
      refreshAllData(true);
    } catch (err: any) {
      notifyError(err.message || 'Failed to add expense.');
    }
  };

  // Settle Payment Handler (Modal Form)
  const handleModalSettleUp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !selectedGroup || !settleToUserId || !settleAmountInput) return;
    try {
      const amountMinor = Math.round(parseFloat(settleAmountInput) * 100);
      await postSettlement(selectedGroup.id, {
        fromUserId: user.id,
        toUserId: settleToUserId,
        amountMinor: amountMinor,
        settlementDate: new Date().toISOString(),
        createdByUserId: user.id
      });

      notifySuccess('Settlement payment recorded successfully!');
      setSettleAmountInput('');
      setSettleToUserId(0);
      setShowSettleModal(false);
      loadGroupData(selectedGroup.id);
      refreshAllData(true);
    } catch (err: any) {
      notifyError(err.message || 'Failed to record settlement.');
    }
  };

  // Helper to resolve Payer Name
  const getPayerName = (expense: Expense): string => {
    if (expense.paidByDisplayName) {
      return expense.paidByUserId === user?.id ? `You (${expense.paidByDisplayName})` : expense.paidByDisplayName;
    }
    if (expense.paidByUserId === user?.id) return 'You';
    const foundFriend = friends.find(f => f.id === expense.paidByUserId);
    if (foundFriend) return foundFriend.displayName;
    return `User #${expense.paidByUserId}`;
  };

  // Category Icon Resolver
  const getCategoryIcon = (category: ExpenseCategory) => {
    switch (category) {
      case 'FOOD': return '🍔';
      case 'TRAVEL': return '✈️';
      case 'UTILITIES': return '💡';
      case 'RENT': return '🏠';
      case 'ENTERTAINMENT': return '🎬';
      case 'SHOPPING': return '🛒';
      default: return '📦';
    }
  };

  // Net Balances Calculations
  const totalYouOweMinor = groupBalances
    .filter(b => b.fromUserId === user?.id)
    .reduce((sum, b) => sum + b.amountMinor, 0);

  const totalOwedToYouMinor = groupBalances
    .filter(b => b.toUserId === user?.id)
    .reduce((sum, b) => sum + b.amountMinor, 0);

  const netBalanceMinor = totalOwedToYouMinor - totalYouOweMinor;
  const pendingRequestsCount = friendRequests.filter(r => r.addressee.id === user?.id && r.status === 'PENDING').length;
  const currentCurrency = selectedGroup?.currencyCode || 'INR';

  // Live split calculation for modal preview
  const parsedExpenseAmount = parseFloat(expenseAmount) || 0;
  const activeMemberCount = groupMembers.length || 1;
  const splitAmountPerPerson = (parsedExpenseAmount / activeMemberCount).toFixed(2);

  return (
    <div className="app-shell">
      {/* Header Bar */}
      <header className="topbar">
        <div className="brand">
          <div className="brand-icon">S</div>
          <span className="brand-title">SettleSense</span>
        </div>

        <div className="topbar-user-section">
          <div className="user-avatar">
            {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
          </div>
          <div className="user-info">
            <span className="user-name">{user?.displayName || 'User'}</span>
            <span className="user-email">{user?.email}</span>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={logout}>
            Sign Out
          </button>
        </div>
      </header>

      {/* Navigation Tabs */}
      <nav className="nav-tabs-bar">
        <button
          className={`tab-btn ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
        >
          🏠 Dashboard
        </button>
        <button
          className={`tab-btn ${activeTab === 'friends' ? 'active' : ''}`}
          onClick={() => setActiveTab('friends')}
        >
          👥 Friends Network
          {pendingRequestsCount > 0 && (
            <span className="badge-count">{pendingRequestsCount}</span>
          )}
        </button>
        <button
          className={`tab-btn ${activeTab === 'groups' ? 'active' : ''}`}
          onClick={() => setActiveTab('groups')}
        >
          📁 Groups ({groups.length})
        </button>
      </nav>

      {/* Main Workspace */}
      <main className="workspace">
        {/* Toast Alerts */}
        {errorMsg && (
          <div className="modal-card" style={{ marginBottom: '20px', backgroundColor: '#881337', borderColor: '#f43f5e' }}>
            <p style={{ color: '#fff', fontWeight: 600 }}>⚠️ {errorMsg}</p>
          </div>
        )}
        {successMsg && (
          <div className="modal-card" style={{ marginBottom: '20px', backgroundColor: '#064e3b', borderColor: '#10b981' }}>
            <p style={{ color: '#fff', fontWeight: 600 }}>✅ {successMsg}</p>
          </div>
        )}

        {/* ================= TAB 1: DASHBOARD OVERVIEW ================= */}
        {activeTab === 'dashboard' && (
          <div>
            <div className="page-header">
              <div className="page-title-group">
                <h1>Financial Dashboard</h1>
                <p>Simple summary of your personal finances and friend network</p>
              </div>
              <div className="quick-actions">
                <button className="btn btn-primary" onClick={() => setShowAddFriendModal(true)}>
                  + Add Friend
                </button>
                <button className="btn btn-secondary" onClick={() => setShowCreateGroupModal(true)}>
                  + Create Group
                </button>
              </div>
            </div>

            {/* Clear Metric Cards */}
            <div className="metrics-grid">
              <div className="metric-card">
                <span className="metric-label">Total Net Position</span>
                <span className={`metric-value ${netBalanceMinor >= 0 ? 'positive' : 'negative'}`}>
                  {netBalanceMinor >= 0 ? '+' : '-'}{formatMoney(Math.abs(netBalanceMinor), currentCurrency)}
                </span>
              </div>
              <div className="metric-card">
                <span className="metric-label">You Owe</span>
                <span className="metric-value negative">
                  {formatMoney(totalYouOweMinor, currentCurrency)}
                </span>
              </div>
              <div className="metric-card">
                <span className="metric-label">You Are Owed</span>
                <span className="metric-value positive">
                  {formatMoney(totalOwedToYouMinor, currentCurrency)}
                </span>
              </div>
              <div className="metric-card">
                <span className="metric-label">Accepted Friends</span>
                <span className="metric-value">{friends.length}</span>
              </div>
            </div>

            <div className="content-grid">
              {/* Friends Summary */}
              <div className="panel">
                <div className="panel-title">
                  <span>👥 Friends Summary</span>
                  <button className="btn btn-secondary btn-sm" onClick={() => setActiveTab('friends')}>
                    Manage Friends
                  </button>
                </div>
                {friends.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-state-icon">👋</div>
                    <h4>No Friends Added Yet</h4>
                    <p>Add friends by email to start sharing expenses in INR.</p>
                    <button className="btn btn-primary btn-sm" style={{ marginTop: '12px' }} onClick={() => setShowAddFriendModal(true)}>
                      Add a Friend
                    </button>
                  </div>
                ) : (
                  <ul className="item-list">
                    {friends.map(f => (
                      <li key={f.id} className="item-card">
                        <div className="item-info">
                          <div className="item-avatar">{f.displayName.charAt(0).toUpperCase()}</div>
                          <div className="item-details">
                            <h4>{f.displayName}</h4>
                            <p>{f.email}</p>
                          </div>
                        </div>
                        <span className="status-badge badge-accepted">FRIEND</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              {/* My Groups List */}
              <div className="panel">
                <div className="panel-title">
                  <span>📁 Your Groups</span>
                  <button className="btn btn-secondary btn-sm" onClick={() => setShowCreateGroupModal(true)}>
                    + New Group
                  </button>
                </div>
                {groups.length === 0 ? (
                  <div className="empty-state">
                    <div className="empty-state-icon">📁</div>
                    <h4>No Active Groups</h4>
                    <p>Create a group with your accepted friends to split expenses.</p>
                  </div>
                ) : (
                  <ul className="item-list">
                    {groups.map(g => (
                      <li
                        key={g.id}
                        className="item-card"
                        style={{ cursor: 'pointer' }}
                        onClick={() => handleSelectGroup(g)}
                      >
                        <div className="item-info">
                          <div className="item-avatar" style={{ background: 'linear-gradient(135deg, #10b981 0%, #6366f1 100%)' }}>
                            {g.name.charAt(0).toUpperCase()}
                          </div>
                          <div className="item-details">
                            <h4>{g.name}</h4>
                            <p>Default: {g.currencyCode}</p>
                          </div>
                        </div>
                        <button className="btn btn-primary btn-sm">
                          Open Group →
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ================= TAB 2: FRIENDS NETWORK ================= */}
        {activeTab === 'friends' && (
          <div>
            <div className="page-header">
              <div className="page-title-group">
                <h1>Friends Network</h1>
                <p>Invite friends by email address to share expenses</p>
              </div>
              <button className="btn btn-primary" onClick={() => setShowAddFriendModal(true)}>
                + Add Friend by Email
              </button>
            </div>

            {/* Pending Requests Banner */}
            {friendRequests.length > 0 && (
              <div className="panel" style={{ borderColor: 'rgba(245, 158, 11, 0.4)' }}>
                <div className="panel-title" style={{ color: '#fbbf24' }}>
                  <span>⏳ Pending Friend Requests ({friendRequests.length})</span>
                </div>
                <ul className="item-list">
                  {friendRequests.map(req => {
                    const isIncoming = req.addressee.id === user?.id;
                    const otherUser = isIncoming ? req.requester : req.addressee;

                    return (
                      <li key={req.id} className="item-card">
                        <div className="item-info">
                          <div className="item-avatar">{otherUser.displayName.charAt(0).toUpperCase()}</div>
                          <div className="item-details">
                            <h4>{otherUser.displayName}</h4>
                            <p>{otherUser.email} • {isIncoming ? 'Sent you a request' : 'Request sent by you'}</p>
                          </div>
                        </div>
                        {isIncoming ? (
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button className="btn btn-primary btn-sm" onClick={() => handleAcceptRequest(req.id)}>
                              Accept
                            </button>
                            <button className="btn btn-danger btn-sm" onClick={() => handleRejectRequest(req.id)}>
                              Decline
                            </button>
                          </div>
                        ) : (
                          <span className="status-badge badge-pending">PENDING</span>
                        )}
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}

            {/* Accepted Friends List */}
            <div className="panel">
              <div className="panel-title">
                <span>Accepted Friends ({friends.length})</span>
              </div>
              {friends.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">👥</div>
                  <h4>No Friends Connected</h4>
                  <p>Send a friend request using an email address to connect.</p>
                  <button className="btn btn-primary btn-sm" style={{ marginTop: '14px' }} onClick={() => setShowAddFriendModal(true)}>
                    Add Friend Now
                  </button>
                </div>
              ) : (
                <ul className="item-list">
                  {friends.map(f => (
                    <li key={f.id} className="item-card">
                      <div className="item-info">
                        <div className="item-avatar">{f.displayName.charAt(0).toUpperCase()}</div>
                        <div className="item-details">
                          <h4>{f.displayName}</h4>
                          <p>{f.email}</p>
                        </div>
                      </div>
                      <span className="status-badge badge-accepted">CONNECTED</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}

        {/* ================= TAB 3: IN-GROUP WORKSPACE ================= */}
        {activeTab === 'groups' && (
          <div>
            <div className="page-header">
              <div className="page-title-group">
                <h1>Group Workspace</h1>
                <p>Select a group to manage transactions</p>
              </div>
              <button className="btn btn-primary" onClick={() => setShowCreateGroupModal(true)}>
                + Create New Group
              </button>
            </div>

            {/* Group Switcher Bar */}
            <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', marginBottom: '24px', paddingBottom: '4px' }}>
              {groups.map(g => (
                <button
                  key={g.id}
                  className={`btn ${selectedGroup?.id === g.id ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => handleSelectGroup(g)}
                >
                  📁 {g.name}
                </button>
              ))}
            </div>

            {selectedGroup ? (
              <div>
                {/* Inside Group Banner with Clean Action Buttons */}
                <div className="panel" style={{ background: 'linear-gradient(135deg, rgba(16,185,129,0.12) 0%, rgba(99,102,241,0.12) 100%)', borderColor: 'var(--primary)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '20px' }}>
                    <div>
                      <h2 style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '4px' }}>
                        {selectedGroup.name}
                      </h2>
                      <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                        Currency: <strong>{selectedGroup.currencyCode}</strong> • Members: <strong>{groupMembers.length}</strong>
                      </p>
                    </div>

                    {/* Prominent Actions: Add Expense & Settle Up */}
                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                      <button
                        className="btn btn-primary btn-lg"
                        onClick={() => {
                          setExpensePaidByUserId(user?.id || 0);
                          setShowAddExpenseModal(true);
                        }}
                      >
                        💳 + Add Expense
                      </button>
                      <button
                        className="btn btn-accent btn-lg"
                        onClick={() => setShowSettleModal(true)}
                      >
                        🤝 Settle Up
                      </button>
                    </div>
                  </div>
                </div>

                {/* GROUP TRANSACTIONS FEED (Clean & Spacious) */}
                <div className="panel">
                  <div className="panel-title">
                    <span>🧾 Group Transactions ({groupExpenses.length})</span>
                  </div>
                  {groupExpenses.length === 0 ? (
                    <div className="empty-state">
                      <div className="empty-state-icon">💸</div>
                      <h4>No Group Transactions Yet</h4>
                      <p>Click <strong>"+ Add Expense"</strong> above to record shared expenses in this group.</p>
                      <button
                        className="btn btn-primary"
                        style={{ marginTop: '16px' }}
                        onClick={() => {
                          setExpensePaidByUserId(user?.id || 0);
                          setShowAddExpenseModal(true);
                        }}
                      >
                        💳 + Add First Expense
                      </button>
                    </div>
                  ) : (
                    <ul className="item-list">
                      {groupExpenses.map(exp => (
                        <li key={exp.id} className="item-card" style={{ padding: '20px 24px' }}>
                          <div className="item-info">
                            <div className="item-avatar" style={{ background: '#1e293b', fontSize: '1.4rem', border: '1px solid var(--border-subtle)' }}>
                              {getCategoryIcon(exp.category)}
                            </div>
                            <div className="item-details">
                              <h4 style={{ fontSize: '1.05rem' }}>{exp.description}</h4>
                              <p style={{ marginTop: '2px' }}>
                                Category: <strong>{exp.category}</strong> • Date: {new Date(exp.expenseDate).toLocaleDateString()}
                              </p>
                              {/* Display User Name who paid */}
                              <div style={{ marginTop: '8px' }}>
                                <span className="status-badge badge-payer">
                                  👤 Paid by: {getPayerName(exp)}
                                </span>
                              </div>
                            </div>
                          </div>
                          <div className="item-meta">
                            <span className="amount-display positive" style={{ fontSize: '1.25rem' }}>
                              {formatMoney(exp.totalMinor, exp.currencyCode)}
                            </span>
                            <span className="status-badge badge-accepted">{exp.status}</span>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            ) : (
              <div className="empty-state">
                <div className="empty-state-icon">📁</div>
                <h4>No Group Selected</h4>
                <p>Create a group or select one above to open its workspace.</p>
              </div>
            )}
          </div>
        )}
      </main>

      {/* ================= MODAL DIALOGS ================= */}

      {/* 1. ADD EXPENSE MODAL */}
      {showAddExpenseModal && selectedGroup && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>💳 Add Group Expense</h3>
              <button className="close-btn" onClick={() => setShowAddExpenseModal(false)}>×</button>
            </div>
            <form onSubmit={handleModalAddExpense}>
              <div className="form-group">
                <label>Description / Title</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Dinner at Olive / Grocery Shopping"
                  value={expenseDesc}
                  onChange={e => setExpenseDesc(e.target.value)}
                  required
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label>Amount</label>
                <div className="amount-input-group">
                  <span className="amount-input-prefix">{getCurrencySymbol(selectedGroup.currencyCode)}</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    className="form-input"
                    placeholder="0.00"
                    value={expenseAmount}
                    onChange={e => setExpenseAmount(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Paid By (User Name)</label>
                <select
                  className="form-select"
                  value={expensePaidByUserId || user?.id || 0}
                  onChange={e => setExpensePaidByUserId(Number(e.target.value))}
                >
                  <option value={user?.id}>You ({user?.displayName})</option>
                  {friends.map(f => (
                    <option key={f.id} value={f.id}>{f.displayName} ({f.email})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Category</label>
                <select
                  className="form-select"
                  value={expenseCategory}
                  onChange={e => setExpenseCategory(e.target.value as ExpenseCategory)}
                >
                  <option value="GENERAL">📦 General</option>
                  <option value="FOOD">🍔 Food & Dining</option>
                  <option value="TRAVEL">✈️ Travel & Transport</option>
                  <option value="UTILITIES">💡 Utilities & Bills</option>
                  <option value="RENT">🏠 Rent & Housing</option>
                  <option value="ENTERTAINMENT">🎬 Entertainment</option>
                  <option value="SHOPPING">🛒 Shopping</option>
                </select>
              </div>

              {/* Live Equal Split Summary Preview */}
              {parsedExpenseAmount > 0 && (
                <div className="split-summary-box">
                  <span className="split-summary-title">
                    Split equally among <strong>{activeMemberCount} member(s)</strong>
                  </span>
                  <span className="split-summary-amount">
                    {getCurrencySymbol(selectedGroup.currencyCode)}{splitAmountPerPerson} / person
                  </span>
                </div>
              )}

              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddExpenseModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Expense
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. SETTLE UP MODAL */}
      {showSettleModal && selectedGroup && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>🤝 Record Settlement Payment</h3>
              <button className="close-btn" onClick={() => setShowSettleModal(false)}>×</button>
            </div>
            <form onSubmit={handleModalSettleUp}>
              <div className="form-group">
                <label>Pay To (Friend Name)</label>
                <select
                  className="form-select"
                  value={settleToUserId}
                  onChange={e => setSettleToUserId(Number(e.target.value))}
                  required
                >
                  <option value={0} disabled>Select Receiver...</option>
                  {friends.map(f => (
                    <option key={f.id} value={f.id}>{f.displayName} ({f.email})</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Payment Amount</label>
                <div className="amount-input-group">
                  <span className="amount-input-prefix">{getCurrencySymbol(selectedGroup.currencyCode)}</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    className="form-input"
                    placeholder="0.00"
                    value={settleAmountInput}
                    onChange={e => setSettleAmountInput(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '24px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowSettleModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-accent">
                  Confirm Settlement Payment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. ADD FRIEND MODAL */}
      {showAddFriendModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Add Friend by Email</h3>
              <button className="close-btn" onClick={() => setShowAddFriendModal(false)}>×</button>
            </div>
            <form onSubmit={handleSendFriendRequest}>
              <div className="form-group">
                <label>Friend's Email Address</label>
                <input
                  type="email"
                  className="form-input"
                  placeholder="e.g. friend@example.com"
                  value={friendEmailInput}
                  onChange={e => setFriendEmailInput(e.target.value)}
                  required
                />
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
                They will receive a friend request. Once accepted, you can invite them to groups!
              </p>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddFriendModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Send Request
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. CREATE GROUP MODAL */}
      {showCreateGroupModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Create New Group</h3>
              <button className="close-btn" onClick={() => setShowCreateGroupModal(false)}>×</button>
            </div>
            <form onSubmit={handleCreateGroup}>
              <div className="form-group">
                <label>Group Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Goa Trip 2026 / Flat Expenses"
                  value={newGroupName}
                  onChange={e => setNewGroupName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label>Currency Code</label>
                <select
                  className="form-select"
                  value={newGroupCurrency}
                  onChange={e => setNewGroupCurrency(e.target.value)}
                >
                  <option value="INR">INR (₹) - Default</option>
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                  <option value="GBP">GBP (£)</option>
                </select>
              </div>

              <div className="form-group">
                <label>Add Members (From Accepted Friends Only)</label>
                {friends.length === 0 ? (
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                    No friends available. Add friends first to invite them to this group!
                  </p>
                ) : (
                  <div className="member-selector-grid">
                    {friends.map(f => (
                      <label key={f.id} className="member-checkbox-label">
                        <input
                          type="checkbox"
                          checked={selectedFriendIdsForGroup.includes(f.id)}
                          onChange={() => {
                            if (selectedFriendIdsForGroup.includes(f.id)) {
                              setSelectedFriendIdsForGroup(selectedFriendIdsForGroup.filter(id => id !== f.id));
                            } else {
                              setSelectedFriendIdsForGroup([...selectedFriendIdsForGroup, f.id]);
                            }
                          }}
                        />
                        <span>{f.displayName}</span>
                      </label>
                    ))}
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '20px' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowCreateGroupModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Create Group
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
