import { useParams } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { api } from '../auth/AuthContext.jsx';
import ChatPanel from '../ide/ChatPanel.jsx';
import FileExplorer from '../ide/FileExplorer.jsx';
import CodeEditor from '../ide/CodeEditor.jsx';
import TerminalPanel from '../ide/TerminalPanel.jsx';
import GitPanel from '../ide/GitPanel.jsx';
import PreviewPanel from '../ide/PreviewPanel.jsx';
import AiActionsPanel from '../ide/AiActionsPanel.jsx';
import DatabasePanel from '../ide/DatabasePanel.jsx';
import BuildStrip from '../ide/BuildStrip.jsx';

const TABS = ['Files', 'Preview', 'Terminal', 'Database', 'Git', 'AI Actions'];
const THEME_KEY = 'aidev_theme';

export default function ProjectWorkspace() {
  const { id } = useParams();
  const projectId = Number(id);
  const [project, setProject] = useState(null);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('Files');
  const [editorMode, setEditorMode] = useState(false);
  const [tabs, setTabs] = useState([]);
  const [activePath, setActivePath] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const [liveActions, setLiveActions] = useState([]);
  const [theme, setTheme] = useState(localStorage.getItem(THEME_KEY) || 'dark');

  useEffect(() => {
    api.get(`/projects/${projectId}`)
      .then(res => {
        setProject(res.data);
        try {
          const rec = JSON.parse(localStorage.getItem('aidev_recent') || '[]')
            .filter(p => p.id !== res.data.id);
          localStorage.setItem('aidev_recent', JSON.stringify([{ id: res.data.id, name: res.data.name }, ...rec].slice(0, 5)));
        } catch { /* ignore */ }
      })
      .catch(err => setError(err.response?.data?.message || 'Failed to load project'));
  }, [projectId]);

  const openFile = (path) => {
    setTabs(prev => prev.includes(path) ? prev : [...prev, path]);
    setActivePath(path);
    setEditorMode(true);
  };

  const closeTab = (path) => {
    setTabs(prev => {
      const next = prev.filter(t => t !== path);
      if (activePath === path) setActivePath(next[next.length - 1] || '');
      return next;
    });
  };

  const bump = () => setRefreshKey(k => k + 1);

  if (error) return <div className="page-loading">{error}</div>;
  if (!project) return <div className="page-loading"><span className="spinner" /></div>;

  return (
    <div className="workspace">
      <div className="workspace-header">
        <div>
          <div className="workspace-title">{project.name}</div>
          <div className="muted">{(project.technologyStack || []).join(' · ')}</div>
        </div>
        <div className="workspace-actions">
          <BuildStrip projectId={projectId} active={liveActions.some(a => a.status === 'RUNNING')} />
          <button className={`btn btn-sm ${editorMode ? 'btn-primary' : ''}`} onClick={() => setEditorMode(!editorMode)}>
            {editorMode ? 'AI Chat' : 'Code Editor'}
          </button>
          <select value={tab} onChange={e => setTab(e.target.value)} className="tab-select">
            {TABS.map(t => <option key={t}>{t}</option>)}
          </select>
        </div>
      </div>

      <div className="workspace-body">
        <div className="workspace-main">
          {editorMode ? (
            <CodeEditor
              projectId={projectId}
              tabs={tabs}
              activePath={activePath}
              onActivate={setActivePath}
              onClose={closeTab}
              onChanged={bump}
              theme={theme}
            />
          ) : (
            <ChatPanel
              projectId={projectId}
              onAction={(a) => setLiveActions(prev => [a, ...prev.filter(x => x.id !== a.id)].slice(0, 50))}
              onFilesChanged={bump}
            />
          )}
        </div>

        <div className="workspace-right">
          <div className="right-tabs">
            {TABS.map(t => (
              <button key={t} className={`right-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>{t}</button>
            ))}
          </div>
          <div className="right-content">
            {tab === 'Files' && <FileExplorer projectId={projectId} onOpen={openFile} refreshKey={refreshKey} />}
            {tab === 'Preview' && <PreviewPanel projectId={projectId} />}
            {tab === 'Terminal' && <TerminalPanel projectId={projectId} />}
            {tab === 'Database' && <DatabasePanel projectId={projectId} />}
            {tab === 'Git' && <GitPanel projectId={projectId} refreshKey={refreshKey} />}
            {tab === 'AI Actions' && <AiActionsPanel projectId={projectId} liveActions={liveActions} />}
          </div>
        </div>
      </div>
    </div>
  );
}
