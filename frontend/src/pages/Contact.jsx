
import { useState } from 'react';
import { Link } from 'react-router-dom';

const INITIAL_FORM = {
  name: '',
  email: '',
  subject: 'General enquiry',
  message: '',
};

export default function Contact() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const updateField = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
    setSubmitted(false);
  };
const submit = (e) => {
  e.preventDefault();
  setError('');
  setSubmitted(false);

  if (
    !form.name.trim() ||
    !form.email.trim() ||
    !form.subject ||
    !form.message.trim()
  ) {
    setError('Please complete all fields.');
    return;
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    setError('Please enter a valid email address.');
    return;
  }

  setSubmitted(true);
};

  return (
    <div className="public-page">
      <header className="public-nav">
        <Link to="/" className="home-brand">
          <span className="home-brand-icon">S</span>
          SmartPay
        </Link>

        <nav aria-label="Main navigation">
          <Link to="/">Home</Link>
          <Link to="/about">About us</Link>
          <Link to="/contact" className="public-nav-active">Contact</Link>
        </nav>

        <div className="public-nav-actions">
          <Link to="/login" className="home-signin">Sign in</Link>
          <Link to="/register" className="btn btn-primary">Get started</Link>
        </div>
      </header>

      <main>
        <section className="contact-hero">
          <span className="home-eyebrow">CONTACT SMARTPAY</span>
          <h1>Let's start a <span>conversation.</span></h1>
          <p>
            Have a question about this project? Use the form below to prepare
            your enquiry. Message delivery will require a connected backend.
          </p>
        </section>

        <section className="contact-content">
          <aside className="contact-info">
            <span className="home-eyebrow">GET IN TOUCH</span>
            <h2>We're here to help you explore the project.</h2>
            <p>
              SmartPay is a portfolio demonstration of a full-stack payment
              management application.
            </p>

            <div className="contact-info-item">
              <span className="contact-info-icon">⌂</span>
              <div>
                <strong>Project information</strong>
                <p>Payment management demo</p>
              </div>
            </div>

            <div className="contact-info-item">
              <span className="contact-info-icon">◇</span>
              <div>
                <strong>Technical stack</strong>
                <p>React.js · Java · Spring Boot · MySQL</p>
              </div>
            </div>

            <div className="contact-demo-note">
              This demo is not a bank or a licensed payment service. Do not
              submit passwords, bank details, or sensitive financial data.
            </div>
          </aside>

          <form className="contact-form" onSubmit={submit} noValidate>
            <h2>Send an enquiry</h2>
            <p className="contact-form-intro">
              Complete the fields below to validate your message.
            </p>

            {error && (
              <div className="contact-notice" role="status">
                {error}
              </div>
            )}

            {submitted && (
              <div className="contact-success" role="status">
                {`Your message has been prepared, but it has not been sent.`}
              </div>
            )}

            <label>
              Full name
              <input
                name="name"
                type="text"
                autoComplete="name"
                placeholder="Enter your name"
                value={form.name}
                onChange={updateField}
                maxLength={100}
                required
              />
            </label>

            <label>
              Email address
              <input
                name="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={updateField}
                maxLength={254}
                required
              />
            </label>

            <label>
              Subject
              <select
                name="subject"
                value={form.subject}
                onChange={updateField}
                required
              >
                <option>General enquiry</option>
                <option>Project information</option>
                <option>Technical feedback</option>
                <option>Other</option>
              </select>
            </label>

            <label>
              Message
              <textarea
                name="message"
                placeholder="Write your message..."
                rows={5}
                value={form.message}
                onChange={updateField}
                maxLength={2000}
                required
              />
            </label>

            <button className="btn btn-primary btn-block" type="submit">
              Validate enquiry <span aria-hidden="true">→</span>
            </button>
          </form>
        </section>
      </main>

      <footer className="home-footer">
        <Link to="/" className="home-brand">
          <span className="home-brand-icon">S</span>
          SmartPay
        </Link>
        <p>A payment platform demo for learning and portfolio purposes.</p>
        <div>
          <Link to="/">Home</Link>
          <Link to="/about">About us</Link>
          <Link to="/login">Sign in</Link>
        </div>
      </footer>
    </div>
  );
}