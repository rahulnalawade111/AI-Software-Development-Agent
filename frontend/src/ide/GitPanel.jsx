import { useEffect, useState } from 'react';
import { gitStatus, gitDiff, gitLog, gitCommit } from '../lib/api.js';

/** Git panel: status (added/modified/deleted), diff, commit, history. */
export default function GitPanel({ projectId, refreshKey }) {
  const [status, setStatus] = useState(null);
  const [diff, setDiff] = useState('');
  const [log, setLog] = useState([]);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const refresh = () => {
    gitStatus(projectId).then(setStatus).catch(e => setError(e.response?.data?.message || 'git error'));
    gitDiff(projectId).then(d => setDiff(d.diff || '')).catch(() => setDiff(''));
    gitLog(projectId).then(setLog).catch(() => setLog([]));
  };

  useEffect(refresh, [projectId, refreshKey]);

  const commit = async (e) => {
    e.preventDefault();
    if (!message.trim() || busy) return;
    setBusy(true);
    try {
      await gitCommit(projectId, message);
      setMessage('');
      refresh();
    } catch (err) { setError(err.response?.data?.message || 'commit failed'); }
    finally { setBusy(false); }
  };

  const fileRow = (label, files, cls) => files?.length ? (
    <div className={`git-group git-${cls}`}>
      <div className="git-group-label">{label}</div>
      {files.map(f => <div key={f} className="git-file">{f}</div>)}
    </div>
  ) : null;

  const clean = status && ['modified', 'added', 'deleted', 'untracked', 'changed']
    .every(k => (status[k] || []).length === 0);

  return (
    <div className="git-panel">
      {error && <div className="banner banner-error">{error}</div>}
      {status && (
        <div className="card git-status">
          <div className="card-title">Working Tree {status.initialized === false && '(not initialized)'}</div>
          {clean && <div className="muted">No changes</div>}
          {fileRow('Modified', status.modified, 'mod')}
          {fileRow('Added', status.added, 'add')}
          {fileRow('Untracked', status.untracked, 'add')}
          {fileRow('Deleted', status.deleted, 'del')}
        </div>
      )}
      <form className="git-commit" onSubmit={commit}>
        <input placeholder="Commit message" value={message} onChange={e => setMessage(e.target.value)} />
        <button className="btn btn-primary" disabled={busy || !message.trim()}>Commit</button>
      </form>
      {diff && (
        <div className="card git-diff-card">
          <div className="card-title">Diff</div>
          <pre className="git-diff">{diff.slice(0, 20000)}</pre>
        </div>
      )}
      <div className="card">
        <div className="card-title">History</div>
        {log.length === 0 && <div className="muted">No commits yet</div>}
        {log.map(c => (
          <div key={c.id || c.hash} className="git-commit-row">
            <span className="git-hash">{String(c.hash || c.id).slice(0, 7)}</span>
            <span className="git-msg">{c.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
