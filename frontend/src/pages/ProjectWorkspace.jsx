import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { api } from '../auth/AuthContext.jsx';

const TABS = ['Files', 'Preview', 'Terminal', 'Database', 'Git', 'AI Actions'];

export default function ProjectWorkspace() {
  const { id } = useParams();
  const [project, setProject] = useState(null);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('Files');
  const [panelOpen, setPanelOpen] = useState(true);

  useEffect(() => {
    api.get(`/projects/${id}`)
      .then(res => {
        setProject(res.data);
        try {
          const rec = JSON.parse(localStorage.getItem('aidev_recent') || '[]')
            .filter(p => p.id !== res.data.id);
          localStorage.setItem('aidev_recent', JSON.stringify([{ id: res.data.id, name: res.data.name }, ...rec].slice(0, 5)));
        } catch { /* ignore */ }
      })
      .catch(err => setError(err.response?.data?.message || 'Failed to load project'));
  }, [id]);

  if (error) return <div className="page-loading">{error}</div>;
  if (!project) return <div className="page-loading"><span className="spinner" /></div>;

  return (
    <div className="workspace">
      <header className="workspace-header">
        <div className="workspace-title">
          <h2>{project.name}</h2>
          <span className="badge">{project.status}</span>
          {(project.techStack || []).map(t => <span key={t} className="badge gray">{t}</span>)}
        </div>
        <button className="btn btn-sm" onClick={() => setPanelOpen(o => !o)}>
          {panelOpen ? 'Hide panel ▸' : '◂ Show panel'}
        </button>
      </header>
      <div className="workspace-body">
        <section className="chat-area">
          <div className="empty-state">
            <strong>AI chat arrives in the next phase</strong>
            <span>Project workspace loaded — workspace directory, file tree and agent chat come with Phase 2–3.</span>
          </div>
        </section>
        {panelOpen && (
          <aside className="right-panel">
            <div className="panel-tabs">
              {TABS.map(t => (
                <button key={t} className={`panel-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>{t}</button>
              ))}
            </div>
            <div className="panel-content">
              <div className="empty-state"><span>{tab} panel — coming in Phase {tab === 'Files' ? 2 : tab === 'AI Actions' ? 3 : '4–7'}</span></div>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}
