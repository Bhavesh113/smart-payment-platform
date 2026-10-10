
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import api, { errorMessage } from '../api/client';
import StatCard from '../components/StatCard';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';
import {
  compactMoney,
  dateTime,
  money,
  shortDate,
  titleCase,
} from '../utils/format';
import { useAuth } from '../context/AuthContext';

export const STATUS_COLORS = {
  SUCCESS: '#1F8A5B',
  FAILED: '#BF3B2B',
  PENDING: '#C98A1B',
  PROCESSING: '#2F6FB2',
  CANCELLED: '#8A979D',
  REVERSED: '#8A979D',
};

export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');

    try {
      const [wallet, analytics, recent, notifications] =
        await Promise.all([
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

  if (error) {
    return <ErrorBox message={error} onRetry={load} />;
  }

  if (!data) {
    return <Spinner label="Loading your dashboard" />;
  }

  const { wallet, analytics: a, recent, notifications } = data;

  const daily = a.transactionsByDate.map((item) => ({
    day: shortDate(item.date),
    count: item.count,
  }));

  const statusData = Object.entries(a.transactionsByStatus)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({ name, value }));

  const firstName = user?.name?.trim()?.split(/\s+/)[0] || 'there';

  return (
    <div className="dashboard-page">
      <header className="page-head dashboard-heading">
        <div>
          <span className="dashboard-eyebrow">YOUR FINANCIAL OVERVIEW</span>
          <h1>Hello, {firstName} 👋</h1>
          <p className="muted-text">
            Here is what is happening with your SmartPay account.
          </p>
        </div>

        <div className="actions">
          <Link className="btn btn-primary" to="/send">
            <span aria-hidden="true">↗</span> Send money
          </Link>
          <Link className="btn btn-ghost" to="/add-money">
            <span aria-hidden="true">＋</span> Add money
          </Link>
        </div>
      </header>

      <section className="balance-slip dashboard-balance" aria-label="Wallet balance">
        <div className="dashboard-balance-content">
          <span className="stat-label on-dark">AVAILABLE WALLET BALANCE</span>
          <strong className="balance-figure">
            {money(wallet.balance, wallet.currency)}
          </strong>
          <span className="on-dark small">
            Wallet {wallet.status.toLowerCase()} · Updated{' '}
            {dateTime(wallet.updatedAt)}
          </span>
        </div>
        <div className="dashboard-balance-decoration" aria-hidden="true">
          ₹
        </div>
      </section>

      <section className="stats dashboard-stats" aria-label="Transaction statistics">
        <StatCard
          label="Total transactions"
          value={a.totalTransactions}
        />
        <StatCard
          label="Successful"
          value={a.successfulTransactions}
          tone="ok"
        />
        <StatCard
          label="Failed"
          value={a.failedTransactions}
          tone="bad"
        />
        <StatCard
          label="Today"
          value={a.todayTransactions}
          hint="transactions"
        />
        <StatCard
          label="Today's amount"
          value={money(a.todayAmount, wallet.currency)}
          hint="successful only"
        />
        <StatCard
          label="Success rate"
          value={`${a.successRate}%`}
          hint={`${a.failureRate}% failed`}
        />
      </section>

      <section className="grid-2 dashboard-charts">
        <div className="card dashboard-card">
          <div className="card-head dashboard-card-heading">
            <div>
              <h2>Transaction activity</h2>
              <p className="muted-text small">
                Transactions over the last 7 days
              </p>
            </div>
            <span className="dashboard-chart-icon" aria-hidden="true">
              ↗
            </span>
          </div>

          {daily.length === 0 ? (
            <EmptyState title="No activity yet">
              Your transaction activity will appear here.
            </EmptyState>
          ) : (
            <div className="chart dashboard-chart">
              <ResponsiveContainer width="100%" height={250}>
                <BarChart
                  data={daily}
                  margin={{ top: 12, right: 8, left: -18, bottom: 0 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="#E4EAF5"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="day"
                    tick={{ fontSize: 12, fill: '#71809B' }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    allowDecimals={false}
                    tick={{ fontSize: 12, fill: '#71809B' }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    cursor={{ fill: '#EEF3FF' }}
                    contentStyle={{
                      borderRadius: 12,
                      border: '1px solid #E0E7F3',
                      boxShadow: '0 8px 24px #172B4D12',
                    }}
                  />
                  <Bar
                    dataKey="count"
                    name="Transactions"
                    fill="#315BFF"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={44}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="card dashboard-card">
          <div className="card-head dashboard-card-heading">
            <div>
              <h2>Payment status</h2>
              <p className="muted-text small">
                Breakdown of your transactions
              </p>
            </div>
            <span className="dashboard-chart-icon" aria-hidden="true">
              ◉
            </span>
          </div>

          {statusData.length === 0 ? (
            <EmptyState title="No transactions yet">
              Add money or send a payment to see activity here.
            </EmptyState>
          ) : (
            <div className="dashboard-status-chart">
              <div className="chart">
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie
                      data={statusData}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={58}
                      outerRadius={84}
                      paddingAngle={3}
                    >
                      {statusData.map((item) => (
                        <Cell
                          key={item.name}
                          fill={STATUS_COLORS[item.name] || '#8A979D'}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value, name) => [
                        value,
                        titleCase(name),
                      ]}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              <ul className="legend dashboard-legend">
                {statusData.map((item) => (
                  <li key={item.name}>
                    <i
                      style={{
                        background:
                          STATUS_COLORS[item.name] || '#8A979D',
                      }}
                    />
                    {titleCase(item.name)}
                    <strong>{item.value}</strong>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </section>

      <section className="grid-2 wide-left dashboard-lower">
        <div className="card dashboard-card">
          <div className="card-head dashboard-card-heading">
            <div>
              <h2>Recent transactions</h2>
              <p className="muted-text small">
                Your latest payment activity
              </p>
            </div>
            <Link className="dashboard-text-link" to="/transactions">
              View all →
            </Link>
          </div>

          {recent.length === 0 ? (
            <EmptyState title="No transactions yet">
              Your latest transactions will appear here.
            </EmptyState>
          ) : (
            <TransactionTable rows={recent} compact />
          )}
        </div>

        <div className="card dashboard-card">
          <div className="card-head dashboard-card-heading">
            <div>
              <h2>Notifications</h2>
              <p className="muted-text small">
                Updates about your account
              </p>
            </div>
            <span className="dashboard-notification-icon" aria-hidden="true">
              ♧
            </span>
          </div>

          {notifications.length === 0 ? (
            <EmptyState title="All caught up">
              Updates will appear here when available.
            </EmptyState>
          ) : (
            <ul className="feed dashboard-feed">
              {notifications.map((notification) => (
                <li key={notification.id}>
                  <span className="dashboard-feed-dot" aria-hidden="true">
                    •
                  </span>
                  <div>
                    <span>{notification.message}</span>
                    <small>{dateTime(notification.createdAt)}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <footer className="dashboard-footer muted-text small">
        Total settled volume:{' '}
        <strong>
          {compactMoney(a.totalAmount)} {wallet.currency}
        </strong>
      </footer>
    </div>
  );
}