import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';
import { ErrorBox } from '../components/States';

export default function Login() {
  const { login } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) return setError('Enter your email and password.');
    setBusy(true);
    setError('');
    try {
      await login(form.email.trim(), form.password);
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <div className="auth">
      <section className="auth-panel">
        <h1 className="brand brand-lg">SmartPay</h1>
        <p className="auth-lead">Every payment is checked, recorded, and confirmed, exactly once.</p>
        <ul className="auth-points">
          <li>Duplicate submissions never charge twice</li>
          <li>Risk rules review each transfer before money moves</li>
          <li>Live status from pending to settled</li>
        </ul>
      </section>
      <form className="auth-form card" onSubmit={submit} noValidate>
        <h2>Sign in</h2>
        <ErrorBox message={error} />
        <label>
          Email
          <input type="email" autoComplete="username" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </label>
        <label>
          Password
          <input type="password" autoComplete="current-password" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </label>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="muted-text center">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </div>
  );
}
