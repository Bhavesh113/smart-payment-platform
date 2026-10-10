import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errorMessage } from '../api/client';
import StatCard from '../components/StatCard';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';
import { compactMoney, dateTime, money, shortDate, titleCase } from '../utils/format';
import { useAuth } from '../context/AuthContext';

export const STATUS_COLORS = {
  SUCCESS: '#1F8A5B', FAILED: '#BF3B2B', PENDING: '#C98A1B', PROCESSING: '#2F6FB2', CANCELLED: '#8A979D', REVERSED: '#8A979D',
};

export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [wallet, analytics, recent, notifications] = await Promise.all([
        api.get('/api/wallet'),
        api.get('/api/analytics/me', { params: { days: 7 } }),
        api.get('/api/transactions', { params: { size: 5 } }),
        api.get('/api/notifications', { params: { limit: 5 } }),
      ]);
      setData({
        wallet: wallet.data.data,
        analytics: analytics.data.data,
        recent: recent.data.data.content,
        notifications: notifications.data.data,
      });
    } catch (err) {
      setError(errorMessage(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!data) return <Spinner label="Loading your dashboard" />;

  const { wallet, analytics: a, recent, notifications } = data;
  const daily = a.transactionsByDate.map((d) => ({ day: shortDate(d.date), count: d.count }));
  const statusData = Object.entries(a.transactionsByStatus)
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name, value }));

  return (
    <>
      <header className="page-head">
        <h1>Hello, {user.name.split(' ')[0]}</h1>
        <div className="actions">
          <Link className="btn btn-primary" to="/send">Send money</Link>
          <Link className="btn btn-ghost" to="/add-money">Add money</Link>
        </div>
      </header>

      <section className="balance-slip" aria-label="Wallet balance">
        <span className="stat-label on-dark">Wallet balance</span>
        <strong className="balance-figure">{money(wallet.balance, wallet.currency)}</strong>
        <span className="on-dark small">Wallet {wallet.status.toLowerCase()} · updated {dateTime(wallet.updatedAt)}</span>
      </section>

      <section className="stats">
        <StatCard label="Total transactions" value={a.totalTransactions} />
        <StatCard label="Successful" value={a.successfulTransactions} tone="ok" />
        <StatCard label="Failed" value={a.failedTransactions} tone="bad" />
        <StatCard label="Today" value={a.todayTransactions} hint="transactions" />
        <StatCard label="Today's amount" value={money(a.todayAmount, wallet.currency)} hint="successful only" />
        <StatCard label="Success rate" value={`${a.successRate}%`} hint={`${a.failureRate}% failed`} />
      </section>

      <section className="grid-2">
        <div className="card">
          <h2>Transactions, last 7 days</h2>
          <div className="chart">
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={daily} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#D8E0DD" vertical={false} />
                <XAxis dataKey="day" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip cursor={{ fill: '#EDF1EF' }} />
                <Bar dataKey="count" name="Transactions" fill="#0C5C4A" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="card">
          <h2>By status</h2>
          {statusData.length === 0 ? (
            <EmptyState title="No transactions yet">Add money or send a payment to see activity here.</EmptyState>
          ) : (
            <div className="chart">
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={statusData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                    {statusData.map((s) => <Cell key={s.name} fill={STATUS_COLORS[s.name]} />)}
                  </Pie>
                  <Tooltip formatter={(v, n) => [v, titleCase(n)]} />
                </PieChart>
              </ResponsiveContainer>
              <ul className="legend">
                {statusData.map((s) => (
                  <li key={s.name}><i style={{ background: STATUS_COLORS[s.name] }} />{titleCase(s.name)} · {s.value}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </section>

      <section className="grid-2 wide-left">
        <div className="card">
          <div className="card-head">
            <h2>Recent transactions</h2>
            <Link to="/transactions">View all</Link>
          </div>
          {recent.length === 0 ? <EmptyState title="Nothing here yet" /> : <TransactionTable rows={recent} compact />}
        </div>
        <div className="card">
          <h2>Notifications</h2>
          {notifications.length === 0 ? (
            <EmptyState title="No notifications">They appear here moments after a payment completes.</EmptyState>
          ) : (
            <ul className="feed">
              {notifications.map((n) => (
                <li key={n.id}>
                  <span>{n.message}</span>
                  <small>{dateTime(n.createdAt)}</small>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
      <p className="muted-text small">Total volume settled: {compactMoney(a.totalAmount)} {wallet.currency}</p>
    </>
  );
}
