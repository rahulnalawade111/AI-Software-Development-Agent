import { useEffect, useRef, useState } from 'react';
import { runTerminal, terminalHistory } from '../lib/api.js';

/** Integrated terminal panel: sandboxed command execution with history. */
export default function TerminalPanel({ projectId }) {
  const [lines, setLines] = useState([]);
  const [command, setCommand] = useState('');
  const [busy, setBusy] = useState(false);
  const [history, setHistory] = useState([]);
  const boxRef = useRef(null);

  useEffect(() => {
    terminalHistory(projectId)
      .then(h => setLines(h.map(r => `$ ${r.command}\n${r.output || ''}`)))
      .catch(() => {});
  }, [projectId]);

  useEffect(() => { boxRef.current?.scrollTo(0, boxRef.current.scrollHeight); }, [lines]);

  const run = async (e) => {
    e.preventDefault();
    const cmd = command.trim();
    if (!cmd || busy) return;
    setCommand('');
    setBusy(true);
    setLines(prev => [...prev, `$ ${cmd}\n`]);
    try {
      const r = await runTerminal(projectId, cmd);
      const out = (r.blocked ? `⛔ ${r.reason}\n` : '') + (r.stdout || '') + (r.stderr ? `\n${r.stderr}` : '') + `\n[exit ${r.exitCode}]\n`;
      setLines(prev => [...prev, out]);
    } catch (err) {
      setLines(prev => [...prev, `error: ${err.response?.data?.message || err.message}\n`]);
    } finally { setBusy(false); }
  };

  return (
    <div className="terminal-panel">
      <div className="terminal-output" ref={boxRef}>
        {lines.length === 0 && <div className="muted">Sandboxed shell — safe commands only (npm, mvn, git, ls, cat…)</div>}
        {lines.map((l, i) => <pre key={i} className={l.startsWith('⛔') ? 'term-blocked' : ''}>{l}</pre>)}
      </div>
      <form className="terminal-input" onSubmit={run}>
        <span className="prompt">$</span>
        <input
          value={command}
          onChange={e => setCommand(e.target.value)}
          disabled={busy}
          placeholder="npm run build"
          autoFocus
        />
      </form>
    </div>
  );
}
