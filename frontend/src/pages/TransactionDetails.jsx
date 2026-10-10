import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { RiskBadge, StatusBadge } from '../components/Badges';
import { ErrorBox, Spinner } from '../components/States';
import { dateTime, money, titleCase } from '../utils/format';

export default function TransactionDetails() {
  const { id } = useParams();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setDetail(null);
    api.get(`/api/transactions/${id}`).then((r) => setDetail(r.data.data)).catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error) return <><ErrorBox message={error} /><Link to="/transactions">Back to transactions</Link></>;
  if (!detail) return <Spinner label="Loading transaction" />;
  const { transaction: t, events } = detail;
  const topUp = t.transactionType === 'ADD_MONEY';

  return (
    <>
      <header className="page-head">
        <div>
          <Link to="/transactions" className="back">← Transactions</Link>
          <h1 className="ref-title">{t.transactionReference}</h1>
        </div>
        <StatusBadge status={t.status} />
      </header>

      <section className="grid-2 wide-left">
        <div className="card">
          <p className="detail-amount">{money(t.amount, t.currency)}</p>
          <dl className="detail-list">
            <div><dt>Type</dt><dd>{topUp ? 'Wallet top-up' : 'Transfer'}</dd></div>
            <div><dt>From</dt><dd>{t.senderName}<small>{t.senderEmail}</small></dd></div>
            {!topUp && <div><dt>To</dt><dd>{t.receiverName}<small>{t.receiverEmail}</small></dd></div>}
            <div><dt>Created</dt><dd>{dateTime(t.createdAt)}</dd></div>
            <div><dt>Last update</dt><dd>{dateTime(t.updatedAt)}</dd></div>
            <div><dt>Risk level</dt><dd><RiskBadge level={t.riskLevel} />{t.riskReasons && <small>{t.riskReasons}</small>}</dd></div>
            {t.description && <div><dt>Note</dt><dd>{t.description}</dd></div>}
            {t.failureReason && <div><dt>Failure reason</dt><dd className="debit">{t.failureReason}</dd></div>}
          </dl>
        </div>
        <div className="card">
          <h2>Timeline</h2>
          <ol className="timeline">
            {events.map((e) => (
              <li key={e.id} className={e.eventStatus === 'FAILURE' ? 'fail' : ''}>
                <strong>{titleCase(e.eventType)}</strong>
                {e.details && <span>{e.details}</span>}
                <small>{dateTime(e.createdAt)}</small>
              </li>
            ))}
          </ol>
        </div>
      </section>
    </>
  );
}
