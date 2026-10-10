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
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    setDetail(null);
    setError('');
    setLoading(true);

    api
      .get(`/api/transactions/${id}`)
      .then((response) => {
        if (active) {
          setDetail(response.data.data);
        }
      })
      .catch((err) => {
        if (active) {
          setError(errorMessage(err));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [id]);

  if (loading) {
    return (
      <div className="transaction-details-loading">
        <Spinner label="Loading transaction" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="transaction-details-page">
        <Link to="/transactions" className="transaction-details-back">
          <span aria-hidden="true">←</span> Back to transactions
        </Link>

        <div className="transaction-details-error">
          <ErrorBox message={error} />
        </div>
      </div>
    );
  }

  if (!detail?.transaction) {
    return (
      <div className="transaction-details-page">
        <Link to="/transactions" className="transaction-details-back">
          <span aria-hidden="true">←</span> Back to transactions
        </Link>

        <div className="transaction-details-empty">
          <h2>Transaction not found</h2>
          <p>We couldn't find the requested transaction details.</p>
        </div>
      </div>
    );
  }

  const { transaction: t, events = [] } = detail;
  const topUp = t.transactionType === 'ADD_MONEY';

  return (
    <div className="transaction-details-page">
      <header className="transaction-details-header">
        <div className="transaction-details-heading">
          <Link to="/transactions" className="transaction-details-back">
            <span aria-hidden="true">←</span> Transactions
          </Link>

          <span className="transaction-details-eyebrow">
            TRANSACTION DETAILS
          </span>

          <h1 className="transaction-details-reference">
            {t.transactionReference}
          </h1>

          <p className="transaction-details-subtitle">
            Review the details and activity for this transaction.
          </p>
        </div>

        <div className="transaction-details-header-status">
          <span className="transaction-details-status-label">
            Current status
          </span>
          <StatusBadge status={t.status} />
        </div>
      </header>

      <section className="transaction-details-grid">
        <div className="card transaction-overview-card">
          <div className="transaction-overview-top">
            <div className="transaction-overview-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <rect
                  x="3"
                  y="5"
                  width="18"
                  height="14"
                  rx="3"
                  stroke="currentColor"
                  strokeWidth="1.7"
                />
                <path
                  d="M3 10H21M7 15H10"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                />
              </svg>
            </div>

            <span className="transaction-overview-type">
              {topUp ? 'Wallet top-up' : 'Money transfer'}
            </span>
          </div>

          <span className="transaction-overview-amount-label">
            Transaction amount
          </span>

          <p className="transaction-overview-amount">
            {money(t.amount, t.currency)}
          </p>

          <div className="transaction-overview-divider" />

          <div className="transaction-details-section-heading">
            <h2>Transaction information</h2>
            <p>Payment and account details</p>
          </div>

          <dl className="transaction-information-list">
            <div className="transaction-information-row">
              <dt>Transaction type</dt>
              <dd>{topUp ? 'Wallet top-up' : 'Transfer'}</dd>
            </div>

            <div className="transaction-information-row">
              <dt>From</dt>
              <dd>
                <span>{t.senderName || '—'}</span>
                {t.senderEmail && <small>{t.senderEmail}</small>}
              </dd>
            </div>

            {!topUp && (
              <div className="transaction-information-row">
                <dt>To</dt>
                <dd>
                  <span>{t.receiverName || '—'}</span>
                  {t.receiverEmail && <small>{t.receiverEmail}</small>}
                </dd>
              </div>
            )}

            <div className="transaction-information-row">
              <dt>Created on</dt>
              <dd>{dateTime(t.createdAt)}</dd>
            </div>

            <div className="transaction-information-row">
              <dt>Last updated</dt>
              <dd>{dateTime(t.updatedAt)}</dd>
            </div>

            <div className="transaction-information-row">
              <dt>Risk level</dt>
              <dd className="transaction-risk-value">
                <RiskBadge level={t.riskLevel} />
                {t.riskReasons && (
                  <small>{t.riskReasons}</small>
                )}
              </dd>
            </div>

            {t.description && (
              <div className="transaction-information-row">
                <dt>Note</dt>
                <dd>{t.description}</dd>
              </div>
            )}

            {t.failureReason && (
              <div className="transaction-information-row">
                <dt>Failure reason</dt>
                <dd className="transaction-failure-reason">
                  {t.failureReason}
                </dd>
              </div>
            )}
          </dl>

          <div className="transaction-reference-footer">
            <span>Transaction reference</span>
            <strong>{t.transactionReference}</strong>
          </div>
        </div>

        <div className="card transaction-timeline-card">
          <div className="transaction-timeline-heading">
            <div className="transaction-timeline-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="9"
                  stroke="currentColor"
                  strokeWidth="1.7"
                />
                <path
                  d="M12 7V12L15.5 14"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>

            <div>
              <h2>Transaction timeline</h2>
              <p>Track the recorded transaction events.</p>
            </div>
          </div>

          {events.length > 0 ? (
            <ol className="transaction-event-list">
              {events.map((event, index) => {
                const failed = event.eventStatus === 'FAILURE';

                return (
                  <li
                    key={event.id ?? `${event.eventType}-${index}`}
                    className={`transaction-event ${
                      failed ? 'is-failed' : ''
                    }`}
                  >
                    <span className="transaction-event-marker">
                      {failed ? (
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          aria-hidden="true"
                        >
                          <path
                            d="M7 7L17 17M17 7L7 17"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                          />
                        </svg>
                      ) : (
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          aria-hidden="true"
                        >
                          <path
                            d="M5 12.5L9.5 17L19 7.5"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      )}
                    </span>

                    <div className="transaction-event-content">
                      <strong>
                        {titleCase(event.eventType)}
                      </strong>

                      {event.details && (
                        <p>{event.details}</p>
                      )}

                      <time dateTime={event.createdAt}>
                        {dateTime(event.createdAt)}
                      </time>
                    </div>
                  </li>
                );
              })}
            </ol>
          ) : (
            <div className="transaction-timeline-empty">
              <p>No timeline events are available for this transaction.</p>
            </div>
          )}

          <div className="transaction-timeline-footer">
            <span className="transaction-timeline-footer-dot" />
            <p>Events shown are those recorded by SmartPay.</p>
          </div>
        </div>
      </section>

      <div className="transaction-details-bottom">
        <Link to="/transactions" className="transaction-details-back-button">
          <span aria-hidden="true">←</span>
          Back to all transactions
        </Link>
      </div>
    </div>
  );
}