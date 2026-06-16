import { Outlet } from 'react-router-dom';

export function AppLayout() {
  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Local v1</p>
          <h1>SettleSense</h1>
        </div>
      </header>
      <Outlet />
    </main>
  );
}
