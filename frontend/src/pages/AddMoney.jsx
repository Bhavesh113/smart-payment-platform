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

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const value = Number(amount);
    if (!amount || Number.isNaN(value) || value <= 0) return setError('Enter an amount greater than zero.');
    if (!/^\d+(\.\d{1,2})?$/.test(amount)) return setError('Use at most 2 decimal places.');
    const payload = { amount: value };
    setBusy(true);
    try {
      const { data } = await api.post('/api/wallet/add-money', payload, { headers: { 'Idempotency-Key': keeper.keyFor(payload) } });
      setResult(data.data);
      keeper.reset();
    } catch (err) {
      if (err.response) keeper.reset();
      setError(fieldErrors(err).amount || errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (result) {
    return (
      <>
        <header className="page-head"><h1>Money added</h1></header>
        <div className="narrow">
          <Receipt result={result} />
          <button className="btn btn-primary btn-block" onClick={() => { setResult(null); setAmount(''); }}>Add more</button>
        </div>
      </>
    );
  }

  return (
    <>
      <header className="page-head"><h1>Add money</h1></header>
      <form className="card narrow" onSubmit={submit} noValidate>
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        <label>
          Amount (INR)
          <input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" />
        </label>
        <div className="chips" role="group" aria-label="Quick amounts">
          {PRESETS.map((p) => (
            <button type="button" key={p} className="chip" onClick={() => setAmount(String(p))}>{money(p)}</button>
          ))}
        </div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Adding…' : 'Add to wallet'}</button>
        <p className="muted-text small">This is a simulation: no real payment gateway is connected.</p>
      </form>
    </>
  );
}
