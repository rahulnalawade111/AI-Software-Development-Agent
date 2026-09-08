import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../auth/AuthContext.jsx';

export default function Templates() {
  const [templates, setTemplates] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    api.get('/templates').then(r => setTemplates(r.data)).catch(() => setTemplates([]));
  }, []);

  async function useTemplate(t) {
    const name = window.prompt('Project name', `${t.name} Project`);
    if (!name) return;
    try {
      const res = await api.post(`/templates/${t.id}/create`, { name, description: t.description });
      navigate(`/project/${res.data.id}`);
    } catch (err) {
      alert(err.response?.data?.message || 'Could not create from template');
    }
  }

  return (
    <div className="page">
      <div className="page-head"><h1>Templates</h1></div>
      {templates === null && <div className="page-loading"><span className="spinner" /></div>}
      {templates && (
        <div className="template-grid">
          {templates.map(t => (
            <div className="card template-card" key={t.id}>
              <h3>{t.name}</h3>
              <p>{t.description}</p>
              <div className="project-meta">{(t.stack || []).map(s => <span key={s} className="badge gray">{s}</span>)}</div>
              <button className="btn btn-primary btn-sm" onClick={() => useTemplate(t)}>Use template</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
