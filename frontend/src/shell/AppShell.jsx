import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { useEffect, useState } from 'react';

const NAV = [
  { to: '/projects/new', label: 'New Project', icon: '＋' },
  { to: '/projects', label: 'Projects', icon: '▦', end: true },
  { to: '/templates', label: 'Templates', icon: '❐' },
  { to: '/settings', label: 'Settings', icon: '⚙' }
];

export default function AppShell({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [recent, setRecent] = useState([]);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    try {
      setRecent(JSON.parse(localStorage.getItem('aidev_recent') || '[]'));
    } catch { setRecent([]); }
  }, []);

  return (
    <div className={`shell ${collapsed ? 'collapsed' : ''}`}>
      <aside className="sidebar">
        <div className="sidebar-head">
          <span className="logo-mark">A</span>
          {!collapsed && <span className="sidebar-title">AI DevAgent</span>}
        </div>
        <nav className="sidebar-nav">
          {NAV.map(item => (
            <NavLink key={item.to} to={item.to} end={item.end}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              title={item.label}>
              <span className="nav-icon">{item.icon}</span>
              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          ))}
          {isAdmin(user) && !collapsed && (
            <div className="recent-group">
              <div className="recent-label">Administration</div>
              <NavLink to="/admin" className="nav-item recent-item">
                <span className="nav-icon">🛡</span><span className="recent-name">Super Admin</span>
              </NavLink>
            </div>
          )}
          {!collapsed && recent.length > 0 && (
            <div className="recent-group">
              <div className="recent-label">Recent Projects</div>
              {recent.slice(0, 5).map(p => (
                <NavLink key={p.id} to={`/project/${p.id}`} className="nav-item recent-item">
                  <span className="nav-icon">▸</span><span className="recent-name">{p.name}</span>
                </NavLink>
              ))}
            </div>
          )}
        </nav>
        <div className="sidebar-foot">
          <div className="user-card">
            <div className="avatar">{user?.name?.[0]?.toUpperCase() || '?'}</div>
            {!collapsed && (
              <div className="user-meta">
                <div className="user-name">{user?.name || user?.email}</div>
                <div className="user-role">{user?.role === 'SUPER_ADMIN' ? 'Super Admin' : 'User'}</div>
              </div>
            )}
          </div>
          <button className="btn btn-ghost btn-sm" onClick={() => { logout(); navigate('/login'); }}>Sign out</button>
        </div>
        <button className="collapse-btn" onClick={() => setCollapsed(c => !c)} title="Toggle sidebar">
          {collapsed ? '»' : '«'}
        </button>
      </aside>

      <main className="main-area">
        <Outlet />
      </main>
    </div>
  );
}
