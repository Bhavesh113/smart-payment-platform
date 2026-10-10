import { useMemo, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/client';
import Receipt from '../components/Receipt';
import { idempotencyKeeper, money } from '../utils/format';

const PRESETS = [500, 1000, 5000, 10000];

export default function AddMoney() {
  const keeper = useMemo(() => idempotencyKeeper('TOPUP'), []);

  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);

  const handleAmountChange = (value) => {
    setAmount(value);
    setError('');
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');

    const value = Number(amount);

    if (!amount.trim() || Number.isNaN(value) || value <= 0) {
      setError('Enter an amount greater than zero.');
      return;
    }

    if (!/^\d+(\.\d{1,2})?$/.test(amount.trim())) {
      setError('Enter a valid amount with at most 2 decimal places.');
      return;
    }

    const payload = { amount: value };

    setBusy(true);

    try {
      const { data } = await api.post(
        '/api/wallet/add-money',
        payload,
        {
          headers: {
            'Idempotency-Key': keeper.keyFor(payload),
          },
        }
      );

      setResult(data.data);
      keeper.reset();
    } catch (err) {
      if (err.response) {
        keeper.reset();
      }

      setError(fieldErrors(err).amount || errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (result) {
    return (
      <div className="add-money-page">
        <header className="page-head add-money-heading">
          <div>
            <span className="add-money-eyebrow">
              <span className="add-money-eyebrow-dot" />
              WALLET MANAGEMENT
            </span>
            <h1>Money added successfully</h1>
            <p>Your wallet top-up request has been completed.</p>
          </div>
        </header>

        <div className="add-money-result">
          <div className="add-money-success-icon">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M5 12.5L9.5 17L19 7.5"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>

          <h2>Top-up complete</h2>
          <p className="add-money-result-description">
            Your transaction receipt is available below.
          </p>

          <Receipt result={result} />

          <button
            type="button"
            className="btn btn-primary btn-block add-money-again"
            onClick={() => {
              setResult(null);
              setAmount('');
              setError('');
            }}
          >
            Add money again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="add-money-page">
      <header className="page-head add-money-heading">
        <div>
          <span className="add-money-eyebrow">
            <span className="add-money-eyebrow-dot" />
            WALLET MANAGEMENT
          </span>
          <h1>Add money</h1>
          <p>Top up your SmartPay wallet in just a few steps.</p>
        </div>
      </header>

      <div className="add-money-layout">
        <form
          className="card add-money-form"
          onSubmit={submit}
          noValidate
        >
          <div className="add-money-section-heading">
            <div className="add-money-section-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M12 5V19M5 12H19"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
              </svg>
            </div>

            <div>
              <h2>Choose an amount</h2>
              <p>Select a quick amount or enter your own.</p>
            </div>
          </div>

          {error && (
            <div className="alert alert-error add-money-error" role="alert">
              {error}
            </div>
          )}

          <label className="add-money-label" htmlFor="topup-amount">
            Amount in Indian rupees (INR)
          </label>

          <div className="add-money-input-wrap">
            <span className="add-money-currency" aria-hidden="true">
              ₹
            </span>

            <input
              id="topup-amount"
              className="add-money-amount-input"
              type="text"
              inputMode="decimal"
              autoComplete="off"
              value={amount}
              onChange={(e) => handleAmountChange(e.target.value)}
              placeholder="0.00"
              aria-invalid={Boolean(error)}
              aria-describedby="topup-help"
            />
          </div>

          <p className="add-money-field-help" id="topup-help">
            Enter a positive amount with up to 2 decimal places.
          </p>

          <div className="add-money-presets-heading">
            <span>Quick select</span>
            <span>INR</span>
          </div>

          <div
            className="add-money-presets"
            role="group"
            aria-label="Quick amounts"
          >
            {PRESETS.map((preset) => (
              <button
                type="button"
                key={preset}
                className={`add-money-preset ${
                  amount === String(preset) ? 'is-selected' : ''
                }`}
                aria-pressed={amount === String(preset)}
                onClick={() => handleAmountChange(String(preset))}
              >
                {money(preset)}
              </button>
            ))}
          </div>

          <div className="add-money-summary">
            <span>Amount to add</span>
            <strong>
              {amount.trim() &&
              Number.isFinite(Number(amount)) &&
              Number(amount) > 0
                ? money(Number(amount))
                : money(0)}
            </strong>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block add-money-submit"
            disabled={busy}
          >
            {busy ? (
              <>
                <span className="add-money-spinner" aria-hidden="true" />
                Processing...
              </>
            ) : (
              <>
                Add to wallet
                <span aria-hidden="true">→</span>
              </>
            )}
          </button>

          <p className="add-money-simulation-note">
            <span aria-hidden="true">ⓘ</span>
            Demo mode: this is a simulated wallet top-up. No real payment
            gateway is connected.
          </p>
        </form>

        <aside className="add-money-sidebar">
          <div className="add-money-info-card">
            <div className="add-money-info-icon">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M12 3L20 6V11.5C20 16.2 16.6 20 12 21C7.4 20 4 16.2 4 11.5V6L12 3Z"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinejoin="round"
                />
                <path
                  d="M9 12L11 14L15.5 9.5"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>

            <h3>Secure wallet management</h3>
            <p>
              SmartPay uses a protected transaction flow for wallet
              operations. This demo does not process real payments.
            </p>
          </div>

          <div className="add-money-steps-card">
            <h3>How it works</h3>

            <div className="add-money-step">
              <span className="add-money-step-number">1</span>
              <div>
                <strong>Choose your amount</strong>
                <p>Select a preset or enter a custom amount.</p>
              </div>
            </div>

            <div className="add-money-step">
              <span className="add-money-step-number">2</span>
              <div>
                <strong>Submit your request</strong>
                <p>SmartPay sends the top-up request to the backend.</p>
              </div>
            </div>

            <div className="add-money-step">
              <span className="add-money-step-number">3</span>
              <div>
                <strong>View your receipt</strong>
                <p>Review the transaction result after processing.</p>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}