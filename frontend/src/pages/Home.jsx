
import { Link } from 'react-router-dom';

const features = [
  {
    icon: '↗',
    title: 'Easy money transfers',
    description:
      'Send money through a simple workflow with transaction status tracking.',
  },
  {
    icon: '◈',
    title: 'Wallet management',
    description:
      'View your wallet balance and manage your available funds in one place.',
  },
  {
    icon: '✓',
    title: 'Transaction tracking',
    description:
      'Review transaction history, statuses, and individual payment details.',
  },
  {
    icon: '⌁',
    title: 'Risk monitoring',
    description:
      'Explore payment risk checks and transaction monitoring in the demo.',
  },
];

export default function Home() {
  return (
    <div className="home-page">
      <header className="home-nav">
        <Link to="/" className="home-brand">
          <span className="home-brand-icon">S</span>
          SmartPay
        </Link>

        <nav className="home-nav-links" aria-label="Main navigation">
          <a href="#features">Features</a>
          <Link to="/about">About us</Link>
          <Link to="/contact">Contact</Link>
        </nav>

        <div className="home-nav-actions">
          <Link to="/login" className="home-signin">
            Sign in
          </Link>
          <Link to="/register" className="btn btn-primary">
            Get started
          </Link>
        </div>
      </header>

      <main>
        <section className="home-hero">
          <div className="home-hero-content">
            <span className="home-eyebrow">
              SMARTER PAYMENTS. CLEARER TRACKING.
            </span>

            <h1>
              Your money.
              <br />
              Your control.
              <br />
              <span>One smart platform.</span>
            </h1>

            <p>
              Manage your wallet, send money, and follow every transaction
              through a clear, easy-to-use payment dashboard.
            </p>

            <div className="home-hero-actions">
              <Link to="/register" className="btn btn-primary">
                Create your account <span aria-hidden="true">→</span>
              </Link>
              <Link to="/login" className="btn btn-ghost">
                Sign in
              </Link>
            </div>

            <div className="home-trust-note">
              <span className="home-trust-dot" />
              A payment platform demo built with React and Spring Boot
            </div>
          </div>

          <div className="home-visual" aria-label="SmartPay dashboard preview">
            <div className="home-orbit home-orbit-one" />
            <div className="home-orbit home-orbit-two" />

            <div className="preview-card">
              <div className="preview-top">
                <div>
                  <span className="preview-label">WALLET OVERVIEW</span>
                  <h2>Your money at a glance</h2>
                </div>
                <span className="preview-mark">S</span>
              </div>

              <div className="preview-balance">
                <span>Available balance</span>
                <strong>₹55,400.00</strong>
                <small>Illustrative demo balance</small>
              </div>

              <div className="preview-actions">
                <div><span>↗</span> Send money</div>
                <div><span>＋</span> Add money</div>
              </div>

              <div className="preview-activity">
                <div className="preview-activity-head">
                  <strong>Recent activity</strong>
                  <span>Demo</span>
                </div>
                <div className="preview-transaction">
                  <span className="preview-transaction-icon">↗</span>
                  <div><strong>Money transfer</strong><small>Transaction tracking</small></div>
                  <b>View</b>
                </div>
                <div className="preview-transaction">
                  <span className="preview-transaction-icon incoming">＋</span>
                  <div><strong>Wallet activity</strong><small>Balance overview</small></div>
                  <b>View</b>
                </div>
              </div>
            </div>

            <div className="home-floating-note">
              <span className="home-floating-check">✓</span>
              <div><strong>Track every step</strong><small>Clear transaction status</small></div>
            </div>
          </div>
        </section>

        <section className="home-features" id="features">
          <div className="home-section-heading">
            <span className="home-eyebrow">BUILT FOR CLARITY</span>
            <h2>Everything you need to manage payments</h2>
            <p>Useful tools, organized in one straightforward experience.</p>
          </div>

          <div className="home-feature-grid">
            {features.map((feature) => (
              <article className="home-feature-card" key={feature.title}>
                <span className="home-feature-icon" aria-hidden="true">
                  {feature.icon}
                </span>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="home-cta">
          <div>
            <span className="home-eyebrow">GET STARTED WITH SMARTPAY</span>
            <h2>Explore a simpler way to manage payments.</h2>
            <p>Create a demo account to explore the platform's features.</p>
          </div>
          <Link to="/register" className="btn home-cta-button">
            Get started <span aria-hidden="true">→</span>
          </Link>
        </section>
      </main>

      <footer className="home-footer">
        <Link to="/" className="home-brand">
          <span className="home-brand-icon">S</span>
          SmartPay
        </Link>
        <p>A payment platform demo for learning and portfolio purposes.</p>
        <div>
          <Link to="/about">About us</Link>
          <Link to="/contact">Contact</Link>
          <Link to="/login">Sign in</Link>
        </div>
      </footer>
    </div>
  );
}