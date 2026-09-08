import { useEffect, useRef, useState } from 'react';
import Editor from '@monaco-editor/react';
import { readFile, writeFile } from '../lib/api.js';

const LANG_BY_EXT = {
  js: 'javascript', jsx: 'javascript', ts: 'typescript', tsx: 'typescript',
  html: 'html', css: 'css', scss: 'scss', json: 'json', md: 'markdown',
  java: 'java', sql: 'sql', yml: 'yaml', yaml: 'yaml', xml: 'xml',
  properties: 'ini', sh: 'shell', py: 'python',
};

function langOf(path) {
  const ext = path.split('.').pop().toLowerCase();
  return LANG_BY_EXT[ext] || 'plaintext';
}

/**
 * Tabbed code editor (Monaco): syntax highlighting, line numbers, multiple
 * tabs, unsaved-changes indicator, Ctrl+S save, find/replace built-in.
 */
export default function CodeEditor({ projectId, tabs, activePath, onActivate, onClose, onChanged, theme }) {
  const editorRef = useRef(null);
  const [buffers, setBuffers] = useState({}); // path -> {content, original, dirty}
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!activePath || buffers[activePath]) return;
    setLoading(true);
    readFile(projectId, activePath)
      .then(d => setBuffers(prev => ({
        ...prev,
        [activePath]: { content: d.content ?? '', original: d.content ?? '', dirty: false },
      })))
      .catch(() => setBuffers(prev => ({ ...prev, [activePath]: { content: '// failed to load', original: '', dirty: false } })))
      .finally(() => setLoading(false));
  }, [activePath]);

  const buf = buffers[activePath];

  const save = async () => {
    if (!buf || !activePath) return;
    setSaving(true);
    try {
      await writeFile(projectId, activePath, buf.content);
      setBuffers(prev => ({ ...prev, [activePath]: { ...prev[activePath], original: buf.content, dirty: false } }));
      onChanged && onChanged(activePath);
    } finally { setSaving(false); }
  };

  const onChange = (value) => {
    setBuffers(prev => ({
      ...prev,
      [activePath]: { ...prev[activePath], content: value ?? '', dirty: value !== prev[activePath].original },
    }));
  };

  // Ctrl+S
  useEffect(() => {
    const onKey = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); save(); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [buf, activePath]);

  return (
    <div className="code-editor">
      <div className="editor-tabs">
        {tabs.map(t => (
          <div
            key={t}
            className={`editor-tab ${t === activePath ? 'active' : ''}`}
            onClick={() => onActivate(t)}
          >
            <span>{t.split('/').pop()}</span>
            {buffers[t]?.dirty && <span className="dirty-dot" title="Unsaved changes">●</span>}
            <button className="tab-close" onClick={(e) => { e.stopPropagation(); onClose(t); }}>×</button>
          </div>
        ))}
        {saving && <span className="muted save-hint">saving…</span>}
      </div>
      {activePath && (
        <Editor
          key={activePath}
          height="100%"
          theme={theme === 'light' ? 'vs' : 'vs-dark'}
          language={langOf(activePath)}
          value={buf?.content ?? ''}
          onChange={onChange}
          loading={<div className="panel-loading">Loading {activePath}…</div>}
          onMount={(ed) => { editorRef.current = ed; }}
          options={{
            minimap: { enabled: true },
            fontSize: 13,
            lineNumbers: 'on',
            automaticLayout: true,
            scrollBeyondLastLine: false,
          }}
        />
      )}
      {!activePath && <div className="editor-empty">Select a file to edit</div>}
    </div>
  );
}
