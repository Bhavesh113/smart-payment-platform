
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';
import { ErrorBox } from '../components/States';

export default function Login() {
  const { login } = useAuth();

  const [form, setForm] = useState({
    email: '',
    password: '',
  });

  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const updateField = (key) => (event) => {
    setForm((previous) => ({
      ...previous,
      [key]: event.target.value,
    }));

    setError('');
  };

  const submit = async (event) => {
    event.preventDefault();

    if (!form.email.trim() || !form.password) {
      setError('Enter your email address and password.');
      return;
    }

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
    <div className="auth register-auth">
      <section className="auth-panel register-panel">
        <Link to="/" className="register-brand">
          <span className="register-brand-icon">S</span>
          SmartPay
        </Link>

        <div className="register-panel-content">
          <span className="register-eyebrow">WELCOME BACK</span>

          <h1 className="register-headline">
            Your money.
            <br />
            Your control.
            <br />
            <span>All in one place.</span>
          </h1>

          <p className="register-description">
            Sign in to manage your wallet, send money, and review your
            transaction activity through your SmartPay dashboard.
          </p>

          <div className="register-benefits">
            <div>
              <span className="register-check">✓</span>
              <span>View your wallet balance</span>
            </div>
            <div>
              <span className="register-check">✓</span>
              <span>Track payment status</span>
            </div>
            <div>
              <span className="register-check">✓</span>
              <span>Review recent transactions</span>
            </div>
          </div>
        </div>

        <p className="register-panel-footer">
          A payment platform demo built with React and Spring Boot.
        </p>
      </section>

      <section className="register-main">
        <div className="register-form-wrap">
          <Link to="/" className="register-mobile-brand">
            <span className="register-brand-icon">S</span>
            SmartPay
          </Link>

          <div className="register-heading">
            <span className="register-eyebrow">YOUR ACCOUNT</span>
            <h2>Sign in to SmartPay</h2>
            <p>Enter your account details to continue.</p>
          </div>

          <form className="register-form" onSubmit={submit} noValidate>
            <ErrorBox message={error} />

            <label className="register-field">
              <span>Email address</span>
              <input
                type="email"
                autoComplete="username"
                placeholder="you@example.com"
                value={form.email}
                onChange={updateField('email')}
                maxLength={254}
                required
              />
            </label>

            <label className="register-field">
              <span>Password</span>

              <div className="register-password-wrap">
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={updateField('password')}
                  required
                />

                <button
                  className="register-password-toggle"
                  type="button"
                  onClick={() =>
                    setShowPassword((previous) => !previous)
                  }
                  aria-label={
                    showPassword ? 'Hide password' : 'Show password'
                  }
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </label>

            <button
              className="btn btn-primary btn-block register-submit"
              type="submit"
              disabled={busy}
            >
              {busy ? 'Signing in…' : 'Sign in'}
              {!busy && <span aria-hidden="true">→</span>}
            </button>

            <p className="register-terms">
              Keep your password private. SmartPay is a portfolio demo;
              do not use real financial information.
            </p>
          </form>

          <div className="register-login">
            New to SmartPay?{' '}
            <Link to="/register">Create an account</Link>
          </div>

          <Link to="/" className="register-back-home">
            ← Back to home
          </Link>
        </div>
      </section>
    </div>
  );
}