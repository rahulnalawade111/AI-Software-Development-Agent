import { useEffect, useState } from 'react';
import { startPreview, stopPreview, previewStatus } from '../lib/api.js';

/** Preview panel: start the app, poll status, embed via iframe. */
export default function PreviewPanel({ projectId }) {
  const [state, setState] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const refresh = () => previewStatus(projectId).then(setState).catch(() => setState({ running: false, port: -1 }));

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 5000);
    return () => clearInterval(t);
  }, [projectId]);

  const start = async () => {
    setBusy(true); setError('');
    try { const r = await startPreview(projectId); setState(r); }
    catch (e) { setError(e.response?.data?.message || 'Failed to start preview'); }
    finally { setBusy(false); }
  };

  const stop = async () => {
    setBusy(true);
    try { await stopPreview(projectId); } finally { setBusy(false); refresh(); }
  };

  const running = state?.running;

  return (
    <div className="preview-panel">
      <div className="preview-toolbar">
        <button className="btn btn-primary" onClick={start} disabled={busy || running}>Start</button>
        <button className="btn" onClick={stop} disabled={busy || !running}>Stop</button>
        {running && <span className="pill pill-green">running :{state.port}</span>}
      </div>
      {error && <div className="banner banner-error">{error}</div>}
      {running ? (
        <iframe
          title="preview"
          src={`/api/projects/${projectId}/preview/`}
          className="preview-frame"
        />
      ) : (
        <div className="preview-empty">
          <div className="preview-icon">▶</div>
          <div>Start the application to preview it here</div>
          <div className="muted">The agent runs the build, starts the server and embeds it live</div>
        </div>
      )}
    </div>
  );
}
