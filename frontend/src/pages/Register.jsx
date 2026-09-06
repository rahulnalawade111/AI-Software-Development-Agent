import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    if (password.length < 8) { setError('Password must be at least 8 characters.'); return; }
    setBusy(true);
    try {
      await register(email, password, name);
      navigate('/projects');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
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
        <p className="auth-sub">Create your AI development workspace</p>
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={onSubmit} className="auth-form">
          <label>Name
            <input className="input" value={name} onChange={e => setName(e.target.value)} required autoFocus />
          </label>
          <label>Email
            <input className="input" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
          </label>
          <label>Password (min 8 chars)
            <input className="input" type="password" value={password} onChange={e => setPassword(e.target.value)} required />
          </label>
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? <span className="spinner" /> : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p>
      </div>
    </div>
  );
}
