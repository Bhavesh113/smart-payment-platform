
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox } from '../components/States';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE = /^\+?[0-9]{10,15}$/;

function validate(form) {
  const errors = {};

  if (form.name.trim().length < 2) {
    errors.name = 'Enter your full name.';
  }

  if (!EMAIL.test(form.email.trim())) {
    errors.email = 'Enter a valid email address.';
  }

  if (
    form.password.length < 8 ||
    !/[A-Za-z]/.test(form.password) ||
    !/\d/.test(form.password)
  ) {
    errors.password = 'Use at least 8 characters with a letter and a digit.';
  }

  if (!PHONE.test(form.phone.trim())) {
    errors.phone = 'Enter 10–15 digits, optionally starting with +.';
  }

  return errors;
}

export default function Register() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
  });

  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const set = (key) => (event) => {
    setForm((previous) => ({
      ...previous,
      [key]: event.target.value,
    }));

    setErrors((previous) => ({
      ...previous,
      [key]: '',
    }));

    setError('');
  };

  const submit = async (event) => {
    event.preventDefault();

    const found = validate(form);
    setErrors(found);
    setError('');

    if (Object.keys(found).length > 0) return;

    setBusy(true);

    try {
      await api.post('/api/auth/register', {
        ...form,
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
      });

      navigate('/login', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  const field = (key, label, props = {}) => (
    <label className="register-field" key={key}>
      <span>{label}</span>
      <input
        value={form[key]}
        onChange={set(key)}
        aria-invalid={Boolean(errors[key])}
        aria-describedby={errors[key] ? `${key}-error` : undefined}
        required
        {...props}
      />
      {errors[key] && (
        <span className="field-error" id={`${key}-error`}>
          {errors[key]}
        </span>
      )}
    </label>
  );

  return (
    <div className="auth register-auth">
      <section className="auth-panel register-panel">
        <Link to="/" className="register-brand">
          <span className="register-brand-icon">S</span>
          SmartPay
        </Link>

        <div className="register-panel-content">
          <span className="register-eyebrow">WELCOME TO SMARTPAY</span>

          <h1 className="register-headline">
            Your money.
            <br />
            Your control.
            <br />
            <span>One smart platform.</span>
          </h1>

          <p className="register-description">
            Create your account to explore wallet management, money transfers,
            and transaction tracking in one place.
          </p>

          <div className="register-benefits">
            <div>
              <span className="register-check">✓</span>
              <span>Manage your wallet balance</span>
            </div>
            <div>
              <span className="register-check">✓</span>
              <span>Track transaction status</span>
            </div>
            <div>
              <span className="register-check">✓</span>
              <span>Review your payment history</span>
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
            <span className="register-eyebrow">GET STARTED</span>
            <h2>Create your account</h2>
            <p>Enter your details below to get started.</p>
          </div>

          <form className="register-form" onSubmit={submit} noValidate>
            <ErrorBox message={error} />

            {field('name', 'Full name', {
              type: 'text',
              autoComplete: 'name',
              placeholder: 'Enter your full name',
              maxLength: 100,
            })}

            {field('email', 'Email address', {
              type: 'email',
              autoComplete: 'email',
              placeholder: 'you@example.com',
              maxLength: 254,
            })}

            {field('phone', 'Phone number', {
              type: 'tel',
              autoComplete: 'tel',
              placeholder: '+919876543210',
              maxLength: 16,
            })}

            <label className="register-field">
              <span>Password</span>
              <div className="register-password-wrap">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={set('password')}
                  autoComplete="new-password"
                  placeholder="At least 8 characters"
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={
                    errors.password ? 'password-error' : 'password-help'
                  }
                  required
                />

                <button
                  className="register-password-toggle"
                  type="button"
                  onClick={() => setShowPassword((previous) => !previous)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>

              {errors.password ? (
                <span className="field-error" id="password-error">
                  {errors.password}
                </span>
              ) : (
                <span className="register-hint" id="password-help">
                  Use at least 8 characters, including a letter and a number.
                </span>
              )}
            </label>

            <button
              className="btn btn-primary btn-block register-submit"
              type="submit"
              disabled={busy}
            >
              {busy ? 'Creating account…' : 'Create account'}
              {!busy && <span aria-hidden="true">→</span>}
            </button>

            <p className="register-terms">
              By creating an account, you can explore the SmartPay demo.
              Do not use real financial or sensitive information.
            </p>
          </form>

          <div className="register-login">
            Already have an account? <Link to="/login">Sign in</Link>
          </div>

          <Link to="/" className="register-back-home">
            ← Back to home
          </Link>
        </div>
      </section>
    </div>
  );
}