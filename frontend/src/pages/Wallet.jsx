import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { ErrorBox, Spinner } from '../components/States';
import { dateTime, money, titleCase } from '../utils/format';

export default function Wallet() {
  const [wallet, setWallet] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    api.get('/api/wallet').then((r) => setWallet(r.data.data)).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <ErrorBox message={error} />;
  if (!wallet) return <Spinner label="Loading wallet" />;
  return (
    <>
      <header className="page-head"><h1>Wallet</h1></header>
      <section className="balance-slip">
        <span className="stat-label on-dark">Available balance</span>
        <strong className="balance-figure">{money(wallet.balance, wallet.currency)}</strong>
        <span className="on-dark small">Balance can never go below zero.</span>
      </section>
      <section className="card">
        <dl className="detail-list">
          <div><dt>Wallet ID</dt><dd>#{wallet.id}</dd></div>
          <div><dt>Currency</dt><dd>{wallet.currency}</dd></div>
          <div><dt>Status</dt><dd>{titleCase(wallet.status)}</dd></div>
          <div><dt>Last updated</dt><dd>{dateTime(wallet.updatedAt)}</dd></div>
        </dl>
        <div className="actions">
          <Link className="btn btn-primary" to="/add-money">Add money</Link>
          <Link className="btn btn-ghost" to="/send">Send money</Link>
        </div>
      </section>
    </>
  );
}
