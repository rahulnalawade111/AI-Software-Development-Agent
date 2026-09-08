import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../auth/AuthContext.jsx';

export default function Projects() {
  const [data, setData] = useState(null);
  const [showArchived, setShowArchived] = useState(false);

  async function load() {
    try {
      const res = await api.get('/projects', { params: { status: showArchived ? 'ARCHIVED' : 'ACTIVE', size: 100 } });
      setData(res.data);
    } catch {
      setData({ content: [] });
    }
  }

  useEffect(() => { load(); }, [showArchived]);

  async function remove(id) {
    if (!window.confirm('Delete this project and its workspace? This cannot be undone.')) return;
    await api.delete(`/projects/${id}`);
    load();
  }

  const projects = data?.content || [];
  return (
    <div className="page">
      <div className="page-head">
        <h1>Projects</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-sm" onClick={() => setShowArchived(a => !a)}>
            {showArchived ? '← Active' : 'Archived'}
          </button>
          <Link className="btn btn-primary" to="/projects/new">＋ New Project</Link>
        </div>
      </div>
      {data === null && <div className="page-loading"><span className="spinner" /></div>}
      {data && projects.length === 0 && (
        <div className="empty-state">
          <strong>{showArchived ? 'No archived projects' : 'No projects yet'}</strong>
          <span>Create your first project and let the AI agent build it for you.</span>
          {!showArchived && <Link className="btn btn-primary" to="/projects/new">Create with AI</Link>}
        </div>
      )}
      {data && projects.length > 0 && (
        <div className="project-grid">
          {projects.map(p => (
            <div className="card project-card" key={p.id}>
              <div className="project-card-head">
                <Link to={`/project/${p.id}`}><h3>{p.name}</h3></Link>
                <span className={`badge ${p.status === 'ACTIVE' ? 'green' : 'gray'}`}>{p.status}</span>
              </div>
              <p className="project-desc">{p.description}</p>
              <div className="project-meta">
                {(Array.isArray(p.techStack) ? p.techStack : []).map(t => <span key={t} className="badge gray">{t}</span>)}
              </div>
              <div className="project-card-actions">
                <Link className="btn btn-sm" to={`/project/${p.id}`}>Open</Link>
                <button className="btn btn-sm" onClick={async () => {
                  const name = window.prompt('Rename project', p.name);
                  if (!name) return;
                  await api.patch(`/projects/${p.id}`, { name });
                  load();
                }}>Rename</button>
                <button className="btn btn-sm" onClick={async () => {
                  await api.post(`/projects/${p.id}/duplicate`);
                  load();
                }}>Duplicate</button>
                <button className="btn btn-sm" onClick={async () => {
                  await api.patch(`/projects/${p.id}/archive`, { archived: p.status !== 'ARCHIVED' });
                  load();
                }}>{p.status === 'ARCHIVED' ? 'Unarchive' : 'Archive'}</button>
                <button className="btn btn-sm btn-danger" onClick={() => remove(p.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
