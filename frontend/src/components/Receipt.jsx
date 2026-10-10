import { Link } from 'react-router-dom';
import { RiskBadge, StatusBadge } from './Badges';
import { dateTime, money } from '../utils/format';

/** A payment slip: shown after a payment or top-up, with the reference users quote to support. */
export default function Receipt({ result }) {
  const t = result.transaction;
  const isTopUp = t.transactionType === 'ADD_MONEY';
  return (
    <section className="receipt" aria-label="Payment receipt">
      <header>
        <StatusBadge status={t.status} />
        <p className="receipt-amount">{money(t.amount, t.currency)}</p>
        <p className="muted-text">{isTopUp ? 'Added to your wallet' : `Sent to ${t.receiverName}`}</p>
      </header>
      <div className="receipt-tear" aria-hidden="true" />
      <dl>
        <div>
          <dt>Reference</dt>
          <dd className="ref">{t.transactionReference}</dd>
        </div>
        {!isTopUp && (
          <div>
            <dt>To</dt>
            <dd>{t.receiverEmail}</dd>
          </div>
        )}
        <div>
          <dt>Date</dt>
          <dd>{dateTime(t.createdAt)}</dd>
        </div>
        {!isTopUp && (
          <div>
            <dt>Risk level</dt>
            <dd>
              <RiskBadge level={t.riskLevel} />
            </dd>
          </div>
        )}
      </dl>
      {result.idempotentReplay && (
        <p className="alert alert-info">
          This request was already processed. No second payment was made; this is the original receipt.
        </p>
      )}
      <Link className="btn btn-ghost btn-block" to={`/transactions/${t.id}`}>
        View timeline
      </Link>
    </section>
  );
}
