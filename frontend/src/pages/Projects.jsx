import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../auth/AuthContext.jsx';
export default function Projects() {
  const [projects, setProjects] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/projects').then(r => setProjects(r.data.content || r.data))
      .catch(() => { setProjects([]); setError('Could not load projects yet (backend projects API arrives in Phase 2).'); });
  }, []);

  return (
    <div className="page">
      <div className="page-head">
        <h1>Projects</h1>
        <Link className="btn btn-primary" to="/projects/new">＋ New Project</Link>
      </div>
      {error && <div className="empty-state">{error}</div>}
      {projects === null && !error && <div className="page-loading"><span className="spinner" /></div>}
      {projects && projects.length === 0 && !error && (
        <div className="empty-state">
          <strong>No projects yet</strong>
          <span>Create your first project and let the AI agent build it for you.</span>
          <Link className="btn btn-primary" to="/projects/new">Create with AI</Link>
        </div>
      )}
      {projects && projects.length > 0 && (
        <div className="project-grid">
          {projects.map(p => (
            <Link to={`/project/${p.id}`} key={p.id} className="card project-card">
              <div className="project-card-head">
                <h3>{p.name}</h3>
                <span className={`badge ${p.status === 'ACTIVE' ? 'green' : 'gray'}`}>{p.status}</span>
              </div>
              <p className="project-desc">{p.description}</p>
              <div className="project-meta">
                {(p.techStack || []).map(t => <span key={t} className="badge gray">{t}</span>)}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
