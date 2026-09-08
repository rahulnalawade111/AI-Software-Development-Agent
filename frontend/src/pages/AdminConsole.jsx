import { useEffect, useState } from 'react';
import {
  adminOverview, adminUsers, adminSetRole, adminDeleteUser,
  adminProjects, adminAiActions,
} from '../lib/api.js';

const SECTIONS = ['Overview', 'Users', 'Projects', 'AI Actions'];

export default function AdminConsole() {
  const [section, setSection] = useState('Overview');
  const [overview, setOverview] = useState(null);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [actions, setActions] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    adminOverview().then(setOverview).catch(e => setError(e.response?.data?.message || 'Failed'));
  }, []);

  useEffect(() => {
    setError('');
    if (section === 'Users' && users.length === 0) {
      adminUsers().then(d => setUsers(d.content || [])).catch(e => setError(e.response?.data?.message || 'Failed to load users'));
    }
    if (section === 'Projects' && projects.length === 0) {
      adminProjects().then(d => setProjects(d.content || [])).catch(e => setError(e.response?.data?.message || 'Failed to load projects'));
    }
    if (section === 'AI Actions' && actions.length === 0) {
      adminAiActions().then(setActions).catch(e => setError(e.response?.data?.message || 'Failed to load actions'));
    }
  }, [section]);

  const changeRole = async (id, role) => {
    try { await adminSetRole(id, role); setUsers(us => us.map(u => u.id === id ? { ...u, roles: [role] } : u)); }
    catch (e) { setError(e.response?.data?.message || 'Failed'); }
  };
  const removeUser = async (id) => {
    if (!window.confirm('Delete this user?')) return;
    try { await adminDeleteUser(id); setUsers(us => us.filter(u => u.id !== id)); }
    catch (e) { setError(e.response?.data?.message || 'Failed'); }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Super Admin</h1>
        <div className="chip-row">
          {SECTIONS.map(s => (
            <button key={s} className={`chip ${section === s ? 'chip-active' : ''}`} onClick={() => setSection(s)}>{s}</button>
          ))}
        </div>
      </div>
      {error && <div className="banner banner-error">{error}</div>}
      {section === 'Overview' && overview && (
        <div className="stat-grid">
          <div className="card stat"><div className="stat-num">{overview.users}</div><div>Users</div></div>
          <div className="card stat"><div className="stat-num">{overview.projects}</div><div>Projects</div></div>
          <div className="card stat"><div className="stat-num">{overview.conversations}</div><div>Conversations</div></div>
          <div className="card stat"><div className="stat-num">{overview.aiMessages}</div><div>AI Messages</div></div>
          <div className="card stat"><div className="stat-num">{overview.aiActions}</div><div>AI Actions</div></div>
        </div>
      )}
      {section === 'Users' && (
        <div className="card table-card">
          <table className="table">
            <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr></thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.id}</td><td>{u.name}</td><td>{u.email}</td>
                  <td>
                    <select value={u.roles?.[0] || 'USER'} onChange={e => changeRole(u.id, e.target.value)}>
                      <option value="USER">USER</option>
                      <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                    </select>
                  </td>
                  <td><button className="btn btn-danger" onClick={() => removeUser(u.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {section === 'Projects' && (
        <div className="card table-card">
          <table className="table">
            <thead><tr><th>ID</th><th>Name</th><th>Status</th><th>Owner</th><th>Created</th></tr></thead>
            <tbody>
              {projects.map(p => (
                <tr key={p.id}><td>{p.id}</td><td>{p.name}</td><td>{p.status}</td><td>{p.ownerId}</td><td>{p.createdAt?.slice(0, 10)}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {section === 'AI Actions' && (
        <div className="card table-card">
          <table className="table">
            <thead><tr><th>ID</th><th>Project</th><th>Type</th><th>Label</th><th>Status</th><th>Time</th></tr></thead>
            <tbody>
              {actions.map(a => (
                <tr key={a.id}><td>{a.id}</td><td>{a.projectId}</td><td>{a.type}</td><td>{a.label}</td>
                  <td><span className={`pill ${a.status === 'SUCCESS' ? 'pill-green' : a.status === 'FAILED' ? 'pill-red' : ''}`}>{a.status}</span></td>
                  <td>{a.createdAt?.slice(11, 19)}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
