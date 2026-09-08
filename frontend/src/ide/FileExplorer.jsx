import { useEffect, useState } from 'react';
import { getFileTree, readFile, writeFile, mkdir, searchCode } from '../lib/api.js';

/** VS Code-style file explorer. Click = open in editor (tab). */
export default function FileExplorer({ projectId, onOpen, refreshKey }) {
  const [tree, setTree] = useState(null);
  const [expanded, setExpanded] = useState(new Set());
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);

  useEffect(() => {
    getFileTree(projectId).then(setTree).catch(() => setTree({ error: 'unavailable' }));
  }, [projectId, refreshKey]);

  const toggle = (path) => {
    setExpanded(prev => {
      const next = new Set(prev);
      next.has(path) ? next.delete(path) : next.add(path);
      return next;
    });
  };

  const runSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) { setResults(null); return; }
    try { setResults(await searchCode(projectId, query)); } catch { setResults([]); }
  };

  const node = (item, depth = 0) => {
    const isDir = item.type === 'directory' || Array.isArray(item.children);
    const open = expanded.has(item.path);
    return (
      <div key={item.path}>
        <div
          className={`file-row ${isDir ? '' : 'file-leaf'}`}
          style={{ paddingLeft: depth * 14 + 8 }}
          onClick={() => isDir ? toggle(item.path) : onOpen(item.path)}
        >
          <span className="file-icon">{isDir ? (open ? '▾' : '▸') : '•'}</span>
          <span>{item.name || item.path.split('/').pop()}</span>
        </div>
        {isDir && open && (item.children || []).map(c => node(c, depth + 1))}
      </div>
    );
  };

  if (!tree) return <div className="panel-loading"><span className="spinner" /></div>;

  return (
    <div className="file-explorer">
      <form className="file-search" onSubmit={runSearch}>
        <input placeholder="Search code…" value={query} onChange={e => setQuery(e.target.value)} />
        <button className="btn btn-sm" type="submit">Go</button>
      </form>
      {results && (
        <div className="search-results">
          {results.length === 0 && <div className="muted">No matches</div>}
          {results.map((r, i) => (
            <div key={i} className="search-hit" onClick={() => onOpen(r.path)}>
              <div className="hit-path">{r.path}:{r.line}</div>
              <div className="hit-text">{r.text?.slice(0, 120)}</div>
            </div>
          ))}
        </div>
      )}
      <div className="tree">
        {tree.error ? <div className="muted">Workspace is empty</div> : node(tree)}
      </div>
    </div>
  );
}
