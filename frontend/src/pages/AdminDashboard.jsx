import { useCallback, useEffect, useState } from 'react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errorMessage } from '../api/client';
import StatCard from '../components/StatCard';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';
import { compactMoney, money, shortDate } from '../utils/format';

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const { data: res } = await api.get('/api/admin/analytics', { params: { days: 14 } });
      setData(res.data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!data) return <Spinner label="Loading platform metrics" />;
  const o = data.overview;
  const series = o.transactionsByDate.map((d) => ({ day: shortDate(d.date), count: d.count, amount: Number(d.amount) }));

  return (
    <>
      <header className="page-head">
        <h1>Platform monitor</h1>
        <button className="btn btn-ghost" onClick={load}>Refresh</button>
      </header>

      <section className="stats">
        <StatCard label="Users" value={data.totalUsers} hint={`${data.activeUsers} active`} />
        <StatCard label="Transactions" value={o.totalTransactions} />
        <StatCard label="Settled volume" value={`₹${compactMoney(o.totalAmount)}`} hint={money(o.totalAmount)} />
        <StatCard label="Success" value={o.successfulTransactions} hint={`${o.successRate}% of all`} tone="ok" />
        <StatCard label="Failed" value={o.failedTransactions} hint={`${o.failureRate}% of all`} tone="bad" />
        <StatCard label="High-risk flagged" value={data.highRiskTransactions} tone={data.highRiskTransactions ? 'bad' : undefined} />
      </section>

      <section className="card">
        <h2>Daily activity, last 14 days</h2>
        <div className="chart">
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={series} margin={{ top: 8, right: 8, left: -10, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#D8E0DD" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={compactMoney} />
              <Tooltip formatter={(v, n) => [n === 'Volume' ? money(v) : v, n]} />
              <Area type="monotone" dataKey="amount" name="Volume" stroke="#0C5C4A" fill="#0C5C4A22" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section className="card">
        <h2>Suspicious and blocked transactions</h2>
        {data.suspiciousTransactions.length === 0
          ? <EmptyState title="Nothing flagged">High and blocked risk transactions appear here.</EmptyState>
          : <TransactionTable rows={data.suspiciousTransactions} />}
      </section>

      <section className="card">
        <h2>Recent transactions</h2>
        {data.recentTransactions.length === 0
          ? <EmptyState title="No transactions yet" />
          : <TransactionTable rows={data.recentTransactions} />}
      </section>
    </>
  );
}
