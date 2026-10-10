
import { Link } from 'react-router-dom';

const principles = [
  {
    icon: '✓',
    title: 'Clarity',
    description:
      'Make wallet balances, transaction details, and payment statuses easy to understand.',
  },
  {
    icon: '◈',
    title: 'Reliability',
    description:
      'Use structured payment workflows and transaction tracking to make activity easier to follow.',
  },
  {
    icon: '⌁',
    title: 'Transparency',
    description:
      'Present payment outcomes and transaction information clearly throughout the experience.',
  },
];

export default function About() {
  return (
    <div className="public-page">
      <header className="public-nav">
        <Link to="/" className="home-brand">
          <span className="home-brand-icon">S</span>
          SmartPay
        </Link>

        <nav aria-label="Main navigation">
          <Link to="/">Home</Link>
          <Link to="/about" className="public-nav-active">About us</Link>
          <Link to="/contact">Contact</Link>
        </nav>

        <div className="public-nav-actions">
          <Link to="/login" className="home-signin">Sign in</Link>
          <Link to="/register" className="btn btn-primary">Get started</Link>
        </div>
      </header>

      <main>
        <section className="about-hero">
          <span className="home-eyebrow">ABOUT SMARTPAY</span>
          <h1>
            Making payment management
            <span> simpler and clearer.</span>
          </h1>
          <p>
            SmartPay is a payment platform demo designed to bring wallet
            management, money transfers, and transaction tracking together
            in one straightforward experience.
          </p>
          <Link to="/register" className="btn btn-primary">
            Explore SmartPay <span aria-hidden="true">→</span>
          </Link>
          <div className="about-hero-decoration" aria-hidden="true">S</div>
        </section>

        <section className="about-story">
          <div className="about-story-label">
            <span className="home-eyebrow">OUR PURPOSE</span>
            <h2>Designed around the payment journey.</h2>
          </div>
          <div className="about-story-copy">
            <p>
              Managing payments involves more than moving money. People also
              need to understand their balances, review past activity, and
              know the status of a transaction.
            </p>
            <p>
              SmartPay brings these tasks into a single interface, with
              dedicated areas for wallets, transfers, transaction history,
              profiles, and administrative monitoring.
            </p>
            <p className="about-demo-note">
              SmartPay is a portfolio and learning project. It is not a bank,
              and it does not represent a licensed payment service.
            </p>
          </div>
        </section>

        <section className="about-principles">
          <div className="home-section-heading">
            <span className="home-eyebrow">WHAT GUIDES THE EXPERIENCE</span>
            <h2>Built for a clearer experience</h2>
            <p>
              Three principles shape how the platform organizes payment
              information and user workflows.
            </p>
          </div>

          <div className="about-principle-grid">
            {principles.map((principle) => (
              <article className="home-feature-card" key={principle.title}>
                <span className="home-feature-icon" aria-hidden="true">
                  {principle.icon}
                </span>
                <h3>{principle.title}</h3>
                <p>{principle.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="about-tech">
          <div>
            <span className="home-eyebrow">THE TECHNOLOGY</span>
            <h2>A full-stack software project</h2>
            <p>
              The application demonstrates frontend development, REST API
              integration, backend services, data persistence, authentication,
              and transaction workflows.
            </p>
          </div>
          <div className="about-tech-tags">
            <span>React.js</span>
            <span>Java</span>
            <span>Spring Boot</span>
            <span>REST APIs</span>
            <span>MySQL</span>
            <span>JWT Authentication</span>
          </div>
        </section>

        <section className="home-cta">
          <div>
            <span className="home-eyebrow">READY TO EXPLORE?</span>
            <h2>See how SmartPay brings payment tools together.</h2>
            <p>Create a demo account to explore the available features.</p>
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
          <Link to="/">Home</Link>
          <Link to="/contact">Contact</Link>
          <Link to="/login">Sign in</Link>
        </div>
      </footer>
    </div>
  );
}
