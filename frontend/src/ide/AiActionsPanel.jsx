import { useEffect, useState } from 'react';
import { getActions } from '../lib/api.js';

/** AI Actions timeline: live agent activity (analyzing → creating → testing → fixing). */
export default function AiActionsPanel({ projectId, liveActions = [] }) {
  const [stored, setStored] = useState([]);

  useEffect(() => {
    getActions(projectId).then(setStored).catch(() => setStored([]));
    const t = setInterval(() => getActions(projectId).then(setStored).catch(() => {}), 4000);
    return () => clearInterval(t);
  }, [projectId]);

  const merged = [...liveActions, ...stored].slice(0, 200);

  const icon = (status) =>
    status === 'SUCCESS' ? '✓' :
    status === 'FAILED' ? '✕' :
    status === 'RUNNING' ? '⟳' :
    status === 'AWAITING_APPROVAL' ? '⏸' : '•';

  return (
    <div className="actions-panel">
      <div className="card-title">Agent Timeline</div>
      {merged.length === 0 && <div className="muted">No AI actions yet — send a requirement in chat</div>}
      <div className="timeline">
        {merged.map((a, i) => (
          <div key={`${a.id ?? 'live'}-${i}`} className={`timeline-row tl-${(a.status || '').toLowerCase()}`}>
            <span className="tl-icon">{icon(a.status)}</span>
            <div className="tl-body">
              <div className="tl-label">{a.label || a.type}</div>
              {a.status && <div className="tl-status">{a.status}</div>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
