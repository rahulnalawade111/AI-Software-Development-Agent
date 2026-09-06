import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const TECH = ['React', 'Spring Boot', 'Node.js', 'Python', 'Java', 'MySQL'];

export default function NewProject() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [tech, setTech] = useState([]);
  const [aiModel, setAiModel] = useState('default');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  function toggleTech(t) {
    setTech(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t]);
  }

  async function onCreate() {
    if (!name.trim()) { setError('Project name is required.'); return; }
    setBusy(true);
    setError('');
    try {
      const { createProject } = await import('../lib/api.js');
      const p = await createProject({ name, description, techStack: tech, aiModel });
      navigate(`/project/${p.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create project (backend projects API arrives in Phase 2).');
      setBusy(false);
    }
  }

  return (
    <div className="page new-project">
      <div className="page-head"><h1>New Project</h1></div>
      <div className="card form-card">
        <label className="form-label">Project name
          <input className="input" value={name} onChange={e => setName(e.target.value)} placeholder="Restaurant Management System" />
        </label>
        <label className="form-label">Description
          <textarea className="textarea" rows={3} value={description} onChange={e => setDescription(e.target.value)}
            placeholder="Build a restaurant management application with super admin, managers, orders, bookings and inventory…" />
        </label>
        <div className="form-label">Technology</div>
        <div className="chip-row">
          {TECH.map(t => (
            <button key={t} type="button" className={`chip ${tech.includes(t) ? 'on' : ''}`} onClick={() => toggleTech(t)}>{t}</button>
          ))}
        </div>
        <div className="form-label">AI Model</div>
        <div className="chip-row">
          {['default', 'advanced'].map(m => (
            <button key={m} type="button" className={`chip ${aiModel === m ? 'on' : ''}`} onClick={() => setAiModel(m)}>
              {m === 'default' ? 'Default AI' : 'Advanced AI'}
            </button>
          ))}
        </div>
        {error && <div className="auth-error">{error}</div>}
        <button className="btn btn-primary btn-lg" onClick={onCreate} disabled={busy}>
          {busy ? <span className="spinner" /> : '⚡ CREATE WITH AI'}
        </button>
      </div>
    </div>
  );
}
