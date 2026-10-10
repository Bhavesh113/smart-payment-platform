import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox } from '../components/States';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validate(f) {
  const e = {};
  if (f.name.trim().length < 2) e.name = 'Enter your full name.';
  if (!EMAIL.test(f.email)) e.email = 'Enter a valid email address.';
  if (f.password.length < 8 || !/[A-Za-z]/.test(f.password) || !/\d/.test(f.password))
    e.password = 'Use at least 8 characters with a letter and a digit.';
  if (!/^\+?[0-9]{10,15}$/.test(f.phone)) e.phone = 'Enter 10–15 digits, optionally starting with +.';
  return e;
}

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    setError('');
    try {
      await api.post('/api/auth/register', { ...form, email: form.email.trim(), name: form.name.trim() });
      navigate('/login', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  const field = (key, label, props = {}) => (
    <label>
      {label}
      <input value={form[key]} onChange={set(key)} aria-invalid={Boolean(errors[key])} {...props} />
      {errors[key] && <span className="field-error">{errors[key]}</span>}
    </label>
  );

  return (
    <div className="auth auth-single">
      <form className="auth-form card" onSubmit={submit} noValidate>
        <h2>Create your account</h2>
        <ErrorBox message={error} />
        {field('name', 'Full name', { autoComplete: 'name' })}
        {field('email', 'Email', { type: 'email', autoComplete: 'email' })}
        {field('phone', 'Phone', { type: 'tel', autoComplete: 'tel', placeholder: '+919876543210' })}
        {field('password', 'Password', { type: 'password', autoComplete: 'new-password' })}
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
        <p className="muted-text center">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  );
}
