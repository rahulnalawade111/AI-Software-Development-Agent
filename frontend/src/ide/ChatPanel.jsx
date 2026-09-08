import { useEffect, useRef, useState } from 'react';
import { api } from '../auth/AuthContext.jsx';
import { listConversations, createConversation, getMessages, approveAction, cancelAction } from '../lib/api.js';

/**
 * AI chat: conversation list, streaming agent responses via SSE,
 * tool-call timeline inline, approval buttons for destructive actions.
 */
export default function ChatPanel({ projectId, onAction, onFilesChanged }) {
  const [conversations, setConversations] = useState([]);
  const [convId, setConvId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [liveActions, setLiveActions] = useState([]);
  const [pendingApproval, setPendingApproval] = useState(null);
  const boxRef = useRef(null);

  useEffect(() => {
    listConversations(projectId)
      .then(list => {
        setConversations(list);
        if (list.length > 0) {
          setConvId(list[0].id);
          getMessages(projectId, list[0].id).then(setMessages).catch(() => {});
        }
      })
      .catch(() => {});
  }, [projectId]);

  useEffect(() => { boxRef.current?.scrollTo(0, boxRef.current.scrollHeight); }, [messages, liveActions]);

  const newConversation = async () => {
    const c = await createConversation(projectId);
    setConversations(prev => [c, ...prev]);
    setConvId(c.id);
    setMessages([]);
    setLiveActions([]);
  };

  const send = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || streaming || !convId) return;
    if (!convId) { await newConversation(); }
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: text }]);
    setStreaming(true);
    setLiveActions([]);

    try {
      const token = localStorage.getItem('aidev_token');
      const res = await fetch(`/api/projects/${projectId}/ai/conversations/${convId}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ message: text }),
      });
      if (!res.ok || !res.body) throw new Error(`chat failed (${res.status})`);

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let assistantText = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split('\n\n');
        buffer = events.pop() || '';
        for (const evt of events) {
          const lines = evt.split('\n');
          const eventType = lines.find(l => l.startsWith('event:'))?.slice(6).trim();
          const dataLine = lines.find(l => l.startsWith('data:'))?.slice(5).trim();
          if (!dataLine) continue;
          let data;
          try { data = JSON.parse(dataLine); } catch { continue; }

          if (eventType === 'token') {
            assistantText += data.content || '';
            setMessages(prev => {
              const next = [...prev];
              const last = next[next.length - 1];
              if (last && last.role === 'assistant' && last.streaming) {
                next[next.length - 1] = { ...last, content: assistantText };
              } else {
                next.push({ role: 'assistant', content: assistantText, streaming: true });
              }
              return next;
            });
          } else if (eventType === 'action') {
            setLiveActions(prev => [data, ...prev.filter(a => a.id !== data.id)]);
            onAction && onAction(data);
            if (data.status === 'AWAITING_APPROVAL') setPendingApproval(data);
            if (data.status === 'RUNNING' || data.status === 'SUCCESS') setPendingApproval(null);
          } else if (eventType === 'files_changed') {
            onFilesChanged && onFilesChanged();
          } else if (eventType === 'done') {
            setMessages(prev => prev.map(m => m.streaming ? { ...m, streaming: false } : m));
          }
        }
      }
    } catch (err) {
      setMessages(prev => [...prev.filter(m => !m.streaming),
        { role: 'assistant', content: `⚠️ ${err.message}` }]);
    } finally {
      setStreaming(false);
      setMessages(prev => prev.map(m => m.streaming ? { ...m, streaming: false } : m));
    }
  };

  const decide = async (action, approved) => {
    setPendingApproval(null);
    try {
      if (approved) await approveAction(projectId, convId, action.id);
      else await cancelAction(projectId, convId, action.id);
    } catch { /* surfaced via events */ }
  };

  return (
    <div className="chat-panel">
      <div className="chat-toolbar">
        <select value={convId ?? ''} onChange={e => {
          setConvId(Number(e.target.value));
          getMessages(projectId, Number(e.target.value)).then(setMessages).catch(() => {});
          setLiveActions([]);
        }}>
          {conversations.length === 0 && <option value="">New conversation</option>}
          {conversations.map(c => <option key={c.id} value={c.id}>{c.title || `Chat #${c.id}`}</option>)}
        </select>
        <button className="btn btn-sm" onClick={newConversation}>+ New chat</button>
      </div>

      <div className="chat-messages" ref={boxRef}>
        {messages.length === 0 && (
          <div className="chat-empty">
            <div className="chat-empty-icon">🤖</div>
            <div>Describe what you want to build…</div>
            <div className="muted">e.g. "Build a restaurant management system using React, Spring Boot and MySQL"</div>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`bubble bubble-${m.role}`}>
            <div className="bubble-role">{m.role === 'user' ? 'You' : 'AI Agent'}</div>
            <pre className="bubble-content">{m.content}</pre>
          </div>
        ))}
        {streaming && <div className="chat-typing">agent is working<span className="dots">…</span></div>}
      </div>

      {pendingApproval && (
        <div className="approval-card">
          <div>⚠️ {pendingApproval.label || 'This action'} needs your approval</div>
          <div className="approval-buttons">
            <button className="btn btn-danger" onClick={() => decide(pendingApproval, false)}>Cancel</button>
            <button className="btn btn-success" onClick={() => decide(pendingApproval, true)}>Approve</button>
          </div>
        </div>
      )}

      <form className="chat-input" onSubmit={send}>
        <textarea
          rows={2}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(e); } }}
          placeholder="Describe the software you want…"
          disabled={streaming}
        />
        <button className="btn btn-primary" disabled={streaming || !input.trim()}>
          {streaming ? 'Working…' : 'Send'}
        </button>
      </form>
    </div>
  );
}
