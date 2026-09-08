import { useEffect, useState } from 'react';
import { api } from '../auth/AuthContext.jsx';

/**
 * Database panel: browse the project's dedicated MySQL schema.
 * Backend routes the query through the same sandboxed, permission-checked
 * database tool the AI uses.
 */
export default function DatabasePanel({ projectId }) {
  const [tables, setTables] = useState(null);
  const [rows, setRows] = useState(null);
  const [selected, setSelected] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/projects/${projectId}/database/schema`)
      .then(setTables)
      .catch(e => setError(e.response?.data?.message || 'Schema unavailable'));
  }, [projectId]);

  const browse = async (table) => {
    setSelected(table);
    setRows(null);
    try {
      const res = await api.post(`/projects/${projectId}/database/query`, {
        sql: `SELECT * FROM ${table} LIMIT 50`,
      });
      setRows(res.data);
    } catch (e) { setError(e.response?.data?.message || 'Query failed'); }
  };

  return (
    <div className="db-panel">
      {error && <div className="banner banner-error">{error}</div>}
      {tables?.error && <div className="muted">{tables.error}</div>}
      <div className="db-layout">
        <div className="db-tables">
          <div className="card-title">Tables</div>
          {(tables?.tables || []).map(t => (
            <div key={t.name}
              className={`db-table ${selected === t.name ? 'active' : ''}`}
              onClick={() => browse(t.name)}>
              {t.name}
              <span className="muted"> ({t.columns?.length || 0} cols)</span>
            </div>
          ))}
          {tables && !tables.error && (tables.tables || []).length === 0 && (
            <div className="muted">No tables yet</div>
          )}
        </div>
        <div className="db-data">
          {rows && (
            <>
              <div className="card-title">{selected} <span className="muted">({rows.rowCount} rows)</span></div>
              <div className="db-grid" style={{ gridTemplateColumns: `repeat(${rows.columns?.length || 1}, minmax(120px, 1fr))` }}>
                {(rows.columns || []).map(c => <div key={c} className="db-cell db-head">{c}</div>)}
                {(rows.rows || []).map((r, i) => (rows.columns || []).map(c => (
                  <div key={`${i}-${c}`} className="db-cell">{String(r[c] ?? '—').slice(0, 100)}</div>
                )))}
              </div>
            </>
          )}
          {!rows && <div className="muted">Select a table to browse data</div>}
        </div>
      </div>
    </div>
  );
}
