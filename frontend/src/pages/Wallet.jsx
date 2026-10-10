
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { ErrorBox, Spinner } from '../components/States';
import { dateTime, money, titleCase } from '../utils/format';

export default function Wallet() {
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/api/wallet')
      .then((response) => setWallet(response.data.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) {
    return <ErrorBox message={error} />;
  }

  if (!wallet) {
    return <Spinner label="Loading wallet" />;
  }

  const walletStatus = titleCase(wallet.status);

  return (
    <div className="wallet-page">
      <header className="page-head wallet-heading">
        <div>
          <span className="dashboard-eyebrow">YOUR MONEY, IN ONE PLACE</span>
          <h1>My wallet</h1>
          <p className="muted-text">
            Manage your available balance and wallet details.
          </p>
        </div>

        <span className="wallet-status">
          <span className="wallet-status-dot" />
          {walletStatus}
        </span>
      </header>

      <section className="balance-slip dashboard-balance wallet-balance">
        <div className="dashboard-balance-content">
          <span className="stat-label on-dark">
            AVAILABLE BALANCE
          </span>

          <strong className="balance-figure">
            {money(wallet.balance, wallet.currency)}
          </strong>

          <span className="on-dark small">
            {wallet.currency} wallet · {walletStatus}
          </span>
        </div>

        <div className="dashboard-balance-decoration" aria-hidden="true">
          ₹
        </div>
      </section>

      <section className="wallet-actions-grid">
        <Link to="/add-money" className="wallet-action-card">
          <span className="wallet-action-icon wallet-icon-blue">
            ＋
          </span>
          <span className="wallet-action-copy">
            <strong>Add money</strong>
            <small>Add funds to your wallet</small>
          </span>
          <span className="wallet-action-arrow" aria-hidden="true">
            →
          </span>
        </Link>

        <Link to="/send" className="wallet-action-card">
          <span className="wallet-action-icon wallet-icon-green">
            ↗
          </span>
          <span className="wallet-action-copy">
            <strong>Send money</strong>
            <small>Make a payment or transfer</small>
          </span>
          <span className="wallet-action-arrow" aria-hidden="true">
            →
          </span>
        </Link>
      </section>

      <section className="card wallet-details-card">
        <div className="card-head dashboard-card-heading">
          <div>
            <h2>Wallet details</h2>
            <p className="muted-text small">
              Information associated with your wallet
            </p>
          </div>

          <span className="dashboard-chart-icon" aria-hidden="true">
            ◈
          </span>
        </div>

        <dl className="detail-list wallet-detail-list">
          <div>
            <dt>Wallet ID</dt>
            <dd className="wallet-id">#{wallet.id}</dd>
          </div>

          <div>
            <dt>Currency</dt>
            <dd>{wallet.currency}</dd>
          </div>

          <div>
            <dt>Wallet status</dt>
            <dd>
              <span className="wallet-inline-status">
                <span className="wallet-status-dot" />
                {walletStatus}
              </span>
            </dd>
          </div>

          <div>
            <dt>Last updated</dt>
            <dd>{dateTime(wallet.updatedAt)}</dd>
          </div>
        </dl>
      </section>

      <section className="wallet-security-note">
        <span className="wallet-security-icon" aria-hidden="true">
          ✓
        </span>
        <div>
          <strong>Your wallet balance is protected</strong>
          <p>
            SmartPay is designed to prevent your wallet balance from
            going below zero. Always review transaction details before
            confirming a payment.
          </p>
        </div>
      </section>
    </div>
  );
}