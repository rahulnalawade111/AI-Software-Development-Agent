import { useEffect, useState } from 'react';

export default function Settings() {
  const [theme, setTheme] = useState(() => localStorage.getItem('aidev_theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('aidev_theme', theme);
  }, [theme]);

  return (
    <div className="page">
      <div className="page-head"><h1>Settings</h1></div>
      <div className="card form-card">
        <h3>Appearance</h3>
        <div className="setting-row">
          <div>
            <strong>Theme</strong>
            <p className="hint">Dark mode is the default developer experience; light mode for bright rooms.</p>
          </div>
          <div className="chip-row">
            <button className={`chip ${theme === 'dark' ? 'on' : ''}`} onClick={() => setTheme('dark')}>🌙 Dark</button>
            <button className={`chip ${theme === 'light' ? 'on' : ''}`} onClick={() => setTheme('light')}>☀️ Light</button>
          </div>
        </div>
        <h3>AI Defaults</h3>
        <div className="setting-row">
          <div>
            <strong>Default AI model</strong>
            <p className="hint">Applies to new projects unless overridden. Managed in Super Admin settings.</p>
          </div>
          <span className="badge purple">Default AI</span>
        </div>
      </div>
    </div>
  );
}
