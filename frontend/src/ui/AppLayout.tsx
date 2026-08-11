import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Production Engine v1</p>
          <h1>SettleSense</h1>
        </div>

        {user && (
          <div className="topbar-user-section" id="user-profile-bar">
            <span className="user-avatar">{user.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}</span>
            <div className="user-info">
              <span className="user-name">{user.displayName}</span>
              <span className="user-email">{user.email}</span>
            </div>
            <button
              id="logout-btn"
              onClick={handleLogout}
              className="btn btn-secondary btn-sm"
              title="Sign out of SettleSense"
            >
              Sign Out
            </button>
          </div>
        )}
      </header>
      <Outlet />
    </main>
  );
}

