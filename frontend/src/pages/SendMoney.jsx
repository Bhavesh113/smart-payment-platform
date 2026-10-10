import { useMemo, useState } from 'react';
import api, { errorMessage, fieldErrors } from '../api/client';
import Receipt from '../components/Receipt';
import { idempotencyKeeper, money } from '../utils/format';
import { Link } from 'react-router-dom';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function SendMoney() {
  const keeper = useMemo(() => idempotencyKeeper('TXN'), []);
  const [form, setForm] = useState({ receiverEmail: '', amount: '', description: '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [failure, setFailure] = useState(null);

  const validate = () => {
    const e = {};
    if (!EMAIL.test(form.receiverEmail.trim())) e.receiverEmail = 'Enter a valid receiver email.';
    const amount = Number(form.amount);
    if (!form.amount || Number.isNaN(amount) || amount <= 0) e.amount = 'Enter an amount greater than zero.';
    else if (!/^\d+(\.\d{1,2})?$/.test(form.amount)) e.amount = 'Use at most 2 decimal places.';
    if (form.description.length > 255) e.description = 'Keep the note under 255 characters.';
    return e;
  };

  const submit = async (e) => {
    e.preventDefault();
    setFailure(null);
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;

    const payload = { receiverEmail: form.receiverEmail.trim().toLowerCase(), amount: Number(form.amount), description: form.description.trim() || null };
    const key = keeper.keyFor(payload); // identical retries reuse the key, so they can never double-charge
    setBusy(true);
    try {
      const { data } = await api.post('/api/payments', payload, { headers: { 'Idempotency-Key': key } });
      setResult(data.data);
      keeper.reset();
    } catch (err) {
      setErrors(fieldErrors(err));
      if (err.response) {
        keeper.reset(); // the server gave a definite answer, so the next attempt is a new payment
        setFailure({ message: errorMessage(err), reference: err.response.data?.transactionReference });
      } else {
        // No answer (timeout / network drop): the payment may have gone through. Keep the key and let the user retry safely.
        setFailure({ message: `${errorMessage(err)} Retrying is safe: your payment will not be charged twice.` });
      }
    } finally {
      setBusy(false);
    }
  };

  const reset = () => {
    setResult(null);
    setFailure(null);
    setForm({ receiverEmail: '', amount: '', description: '' });
  };

  if (result) {
    return (
      <>
        <header className="page-head"><h1>Payment sent</h1></header>
        <div className="narrow">
          <Receipt result={result} />
          <button className="btn btn-primary btn-block" onClick={reset}>Send another payment</button>
        </div>
      </>
    );
  }

  return (
    <>
      <header className="page-head"><h1>Send money</h1></header>
      <form className="card narrow" onSubmit={submit} noValidate>
        {failure && (
          <div className="alert alert-error" role="alert">
            <span>
              {failure.message}
              {failure.reference && <> Reference: <span className="ref">{failure.reference}</span>. <Link to="/transactions">See transactions</Link></>}
            </span>
          </div>
        )}
        <label>
          Receiver email
          <input type="email" value={form.receiverEmail} aria-invalid={Boolean(errors.receiverEmail)}
            onChange={(e) => setForm({ ...form, receiverEmail: e.target.value })} placeholder="bob@example.com" />
          {errors.receiverEmail && <span className="field-error">{errors.receiverEmail}</span>}
        </label>
        <label>
          Amount (INR)
          <input inputMode="decimal" value={form.amount} aria-invalid={Boolean(errors.amount)}
            onChange={(e) => setForm({ ...form, amount: e.target.value })} placeholder="0.00" />
          {errors.amount && <span className="field-error">{errors.amount}</span>}
        </label>
        <label>
          Note <span className="muted-text">(optional)</span>
          <textarea rows={3} value={form.description} maxLength={255}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
          {errors.description && <span className="field-error">{errors.description}</span>}
        </label>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Processing payment…' : form.amount && Number(form.amount) > 0 ? `Send ${money(form.amount)}` : 'Send money'}
        </button>
      </form>
    </>
  );
}
