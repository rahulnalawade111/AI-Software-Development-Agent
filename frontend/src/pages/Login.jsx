import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(email, password);
      navigate('/projects');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <div className="auth-logo">
          <span className="logo-mark">A</span>
          <h1>AI DevAgent</h1>
        </div>
        <p className="auth-sub">Sign in to your AI software development workspace</p>
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={onSubmit} className="auth-form">
          <label>Email
            <input className="input" type="email" value={email} onChange={e => setEmail(e.target.value)} required autoFocus />
          </label>
          <label>Password
            <input className="input" type="password" value={password} onChange={e => setPassword(e.target.value)} required />
          </label>
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : 'Sign in'}
          </button>
        </form>
        <p className="auth-switch">No account? <Link to="/register">Create one</Link></p>
        <div className="auth-demo-hint">
          <span>Demo: demo@aidev.local / Demo#12345</span>
        </div>
      </div>
    </div>
  );
}
