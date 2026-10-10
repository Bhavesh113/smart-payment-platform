import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/send', label: 'Send money' },
  { to: '/add-money', label: 'Add money' },
  { to: '/wallet', label: 'Wallet' },
  { to: '/transactions', label: 'Transactions' },
  { to: '/profile', label: 'Profile' },
];

export default function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const signOut = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="shell">
      <header className="topbar">
        <button className="menu-btn" aria-label="Toggle navigation" aria-expanded={open} onClick={() => setOpen(!open)}>
          <span />
          <span />
          <span />
        </button>
        <span className="brand">SmartPay</span>
      </header>

      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand brand-lg">SmartPay</div>
        <nav onClick={() => setOpen(false)}>
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {n.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
              Admin monitor
            </NavLink>
          )}
        </nav>
        <div className="sidebar-foot">
          <div>
            <strong>{user?.name}</strong>
            <small>{user?.email}</small>
          </div>
          <button className="btn btn-ghost btn-sm on-dark" onClick={signOut}>
            Sign out
          </button>
        </div>
      </aside>
      {open && <div className="scrim" onClick={() => setOpen(false)} />}

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
