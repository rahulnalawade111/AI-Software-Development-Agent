import { useEffect, useState } from 'react';
import { buildHistory } from '../lib/api.js';

/**
 * Build attempts strip: "Build #1 ❌ / Build #2 ✓" — polls build history
 * while the agent works, showing the BUILD → FIX → BUILD loop outcome.
 */
export default function BuildStrip({ projectId, active }) {
  const [runs, setRuns] = useState([]);

  useEffect(() => {
    let alive = true;
    const load = () => buildHistory(projectId)
      .then(h => { if (alive) setRuns((h || []).slice(0, 8).reverse()); })
      .catch(() => {});
    load();
    const t = active ? setInterval(load, 4000) : null;
    return () => { alive = false; if (t) clearInterval(t); };
  }, [projectId, active]);

  if (runs.length === 0) return null;

  return (
    <div className="build-strip">
      {runs.map((r, i) => {
        const ok = r.status === 'SUCCESS';
        const attempt = r.attempt || (i + 1);
        return (
          <span key={r.id || i} className={`build-badge ${ok ? 'ok' : 'fail'}`} title={r.command || ''}>
            Build #{attempt} {ok ? '✓' : '❌'}
          </span>
        );
      })}
    </div>
  );
}
