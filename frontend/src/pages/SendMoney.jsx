
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../api/client';
import Receipt from '../components/Receipt';
import { idempotencyKeeper, money } from '../utils/format';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function SendMoney() {
  const keeper = useMemo(() => idempotencyKeeper('TXN'), []);

  const [form, setForm] = useState({
    receiverEmail: '',
    amount: '',
    description: '',
  });

  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [failure, setFailure] = useState(null);

  const updateField = (key) => (event) => {
    setForm((previous) => ({
      ...previous,
      [key]: event.target.value,
    }));

    setErrors((previous) => ({
      ...previous,
      [key]: '',
    }));

    setFailure(null);
  };

  const validate = () => {
    const found = {};

    if (!EMAIL.test(form.receiverEmail.trim())) {
      found.receiverEmail = 'Enter a valid receiver email address.';
    }

    const amount = Number(form.amount);

    if (!form.amount.trim() || !Number.isFinite(amount) || amount <= 0) {
      found.amount = 'Enter an amount greater than zero.';
    } else if (!/^\d+(\.\d{1,2})?$/.test(form.amount.trim())) {
      found.amount = 'Use at most 2 decimal places.';
    }

    if (form.description.length > 255) {
      found.description = 'Keep the note under 255 characters.';
    }

    return found;
  };

  const submit = async (event) => {
    event.preventDefault();
    setFailure(null);

    const found = validate();
    setErrors(found);

    if (Object.keys(found).length > 0) {
      return;
    }

    const payload = {
      receiverEmail: form.receiverEmail.trim().toLowerCase(),
      amount: Number(form.amount),
      description: form.description.trim() || null,
    };

    // Identical retries reuse the same key to protect against duplicate payments.
    const key = keeper.keyFor(payload);

    setBusy(true);

    try {
      const { data } = await api.post('/api/payments', payload, {
        headers: { 'Idempotency-Key': key },
      });

      setResult(data.data);
      keeper.reset();
    } catch (err) {
      setErrors(fieldErrors(err));

      if (err.response) {
        keeper.reset();

        setFailure({
          message: errorMessage(err),
          reference: err.response.data?.transactionReference,
          retrySafe: false,
        });
      } else {
        // A network error does not confirm whether the payment completed.
        // Keep the idempotency key so a retry can be made safely.
        setFailure({
          message: errorMessage(err),
          retrySafe: true,
        });
      }
    } finally {
      setBusy(false);
    }
  };

  const reset = () => {
    setResult(null);
    setFailure(null);
    setErrors({});
    setForm({
      receiverEmail: '',
      amount: '',
      description: '',
    });
  };

  if (result) {
    return (
      <div className="send-page">
        <header className="page-head">
          <div>
            <span className="dashboard-eyebrow">PAYMENT UPDATE</span>
            <h1>Payment submitted</h1>
            <p className="muted-text">
              Review your payment receipt and transaction details below.
            </p>
          </div>
        </header>

        <div className="send-result">
          <div className="send-result-icon" aria-hidden="true">
            ✓
          </div>

          <h2>Payment response received</h2>

          <p className="muted-text">
            Check the receipt for the transaction status. A submitted payment
            is not necessarily settled immediately.
          </p>

          <Receipt result={result} />

          <div className="send-result-actions">
            <button
              type="button"
              className="btn btn-primary"
              onClick={reset}
            >
              Send another payment
            </button>

            <Link className="btn btn-ghost" to="/transactions">
              View transactions
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="send-page">
      <header className="page-head send-heading">
        <div>
          <span className="dashboard-eyebrow">FAST. CLEAR. TRACKABLE.</span>
          <h1>Send money</h1>
          <p className="muted-text">
            Transfer money to another SmartPay account.
          </p>
        </div>
      </header>

      <div className="send-layout">
        <section className="card send-form-card">
          <div className="card-head dashboard-card-heading">
            <div>
              <h2>Transfer details</h2>
              <p className="muted-text small">
                Enter the recipient and payment amount.
              </p>
            </div>

            <span className="dashboard-chart-icon" aria-hidden="true">
              ↗
            </span>
          </div>

          {failure && (
            <div className="alert alert-error send-error" role="alert">
              <div>
                <strong>Payment could not be confirmed</strong>
                <p>{failure.message}</p>

                {failure.retrySafe && (
                  <p>
                    This may be a network issue. Retrying the same payment
                    is protected by the existing idempotency key. Check your
                    transaction history if you are unsure of its status.
                  </p>
                )}

                {failure.reference && (
                  <p>
                    Reference:{' '}
                    <span className="ref">{failure.reference}</span>
                  </p>
                )}

                <Link to="/transactions">Review transactions</Link>
              </div>
            </div>
          )}

          <form className="send-form" onSubmit={submit} noValidate>
            <label className="send-field">
              <span>Recipient email</span>
              <input
                type="email"
                autoComplete="email"
                placeholder="recipient@example.com"
                value={form.receiverEmail}
                onChange={updateField('receiverEmail')}
                aria-invalid={Boolean(errors.receiverEmail)}
                aria-describedby={
                  errors.receiverEmail ? 'send-email-error' : undefined
                }
                maxLength={254}
                required
              />

              {errors.receiverEmail && (
                <span id="send-email-error" className="field-error">
                  {errors.receiverEmail}
                </span>
              )}
            </label>

            <label className="send-field">
              <span>Amount</span>

              <div className="send-amount-wrap">
                <span className="send-currency" aria-hidden="true">
                  ₹
                </span>

                <input
                  type="number"
                  inputMode="decimal"
                  min="0.01"
                  step="0.01"
                  placeholder="0.00"
                  value={form.amount}
                  onChange={updateField('amount')}
                  aria-invalid={Boolean(errors.amount)}
                  aria-describedby={
                    errors.amount ? 'send-amount-error' : 'send-amount-hint'
                  }
                  required
                />
              </div>

              {errors.amount ? (
                <span id="send-amount-error" className="field-error">
                  {errors.amount}
                </span>
              ) : (
                <small id="send-amount-hint">
                  Enter the amount in Indian rupees.
                </small>
              )}
            </label>

            <label className="send-field">
              <span>
                Payment note <span className="muted-text">(optional)</span>
              </span>

              <textarea
                rows={4}
                maxLength={255}
                placeholder="What is this payment for?"
                value={form.description}
                onChange={updateField('description')}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={
                  errors.description ? 'send-note-error' : undefined
                }
              />

              <div className="send-note-footer">
                {errors.description ? (
                  <span id="send-note-error" className="field-error">
                    {errors.description}
                  </span>
                ) : (
                  <small>Add a short note for your records.</small>
                )}

                <small>{form.description.length}/255</small>
              </div>
            </label>

            {form.amount && Number(form.amount) > 0 && (
              <div className="send-summary">
                <span>Payment amount</span>
                <strong>{money(form.amount, 'INR')}</strong>
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-block send-submit"
              disabled={busy}
            >
              {busy
                ? 'Processing payment…'
                : form.amount && Number(form.amount) > 0
                  ? `Send ${money(form.amount, 'INR')} →`
                  : 'Continue to payment →'}
            </button>

            <p className="send-disclaimer">
              Please verify the recipient email and amount before submitting.
              Do not retry a payment with changed details until you have
              checked the status of any earlier attempt.
            </p>
          </form>
        </section>

        <aside className="send-side">
          <div className="send-info-card">
            <span className="send-info-icon" aria-hidden="true">
              ✓
            </span>

            <h2>Designed for safer transfers</h2>

            <p>
              SmartPay uses an idempotency key to help prevent duplicate
              processing when an identical request is retried.
            </p>
          </div>

          <div className="send-steps-card">
            <h2>Before you send</h2>

            <ol>
              <li>
                <span>1</span>
                <div>
                  <strong>Verify the recipient</strong>
                  <small>Check the email address carefully.</small>
                </div>
              </li>

              <li>
                <span>2</span>
                <div>
                  <strong>Check the amount</strong>
                  <small>Confirm the amount and payment note.</small>
                </div>
              </li>

              <li>
                <span>3</span>
                <div>
                  <strong>Review the result</strong>
                  <small>Use your receipt and transaction history.</small>
                </div>
              </li>
            </ol>
          </div>
        </aside>
      </div>
    </div>
  );
}
