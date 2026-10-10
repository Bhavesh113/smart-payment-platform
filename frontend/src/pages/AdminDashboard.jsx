import { useCallback, useEffect, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import api, { errorMessage } from '../api/client';
import StatCard from '../components/StatCard';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';
import { compactMoney, money, shortDate } from '../utils/format';

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setError('');
    setRefreshing(true);

    try {
      const { data: response } = await api.get('/api/admin/analytics', {
        params: { days: 14 },
      });

      setData(response.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (error && !data) {
    return (
      <div className="admin-dashboard-page">
        <header className="page-head admin-dashboard-heading">
          <div>
            <span className="admin-dashboard-eyebrow">
              ADMINISTRATION
            </span>
            <h1>Platform monitor</h1>
            <p>Monitor platform activity and transaction health.</p>
          </div>
        </header>

        <ErrorBox message={error} onRetry={load} />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="admin-dashboard-loading">
        <Spinner label="Loading platform metrics" />
      </div>
    );
  }

  const overview = data.overview;
  const series = (overview.transactionsByDate || []).map((item) => ({
    day: shortDate(item.date),
    count: item.count,
    amount: Number(item.amount),
  }));

  return (
    <div className="admin-dashboard-page">
      <header className="page-head admin-dashboard-heading">
        <div>
          <span className="admin-dashboard-eyebrow">
            <span className="admin-dashboard-live-dot" />
            ADMINISTRATION
          </span>

          <h1>Platform monitor</h1>

          <p>
            Monitor platform activity, payment performance, and risk indicators.
          </p>
        </div>

        <button
          type="button"
          className="btn btn-ghost admin-dashboard-refresh"
          onClick={load}
          disabled={refreshing}
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
          >
            <path
              d="M20 7V3M20 7H16M20 7A8 8 0 0 0 5.4 5.4M4 17V21M4 17H8M4 17A8 8 0 0 0 18.6 18.6"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          {refreshing ? 'Refreshing...' : 'Refresh data'}
        </button>
      </header>

      {error && (
        <div className="admin-dashboard-inline-error">
          <ErrorBox message={error} onRetry={load} />
        </div>
      )}

      <section className="admin-dashboard-section">
        <div className="admin-dashboard-section-heading">
          <div>
            <h2>Platform overview</h2>
            <p>Current metrics returned by the analytics service.</p>
          </div>

          <span className="admin-dashboard-period">
            14-day activity
          </span>
        </div>

        <div className="stats admin-dashboard-stats">
          <StatCard
            label="Total users"
            value={data.totalUsers}
            hint={`${data.activeUsers} active`}
          />

          <StatCard
            label="Transactions"
            value={overview.totalTransactions}
          />

          <StatCard
            label="Settled volume"
            value={`₹${compactMoney(overview.totalAmount)}`}
            hint={money(overview.totalAmount)}
          />

          <StatCard
            label="Successful"
            value={overview.successfulTransactions}
            hint={`${overview.successRate}% of all`}
            tone="ok"
          />

          <StatCard
            label="Failed"
            value={overview.failedTransactions}
            hint={`${overview.failureRate}% of all`}
            tone="bad"
          />

          <StatCard
            label="High-risk flagged"
            value={data.highRiskTransactions}
            tone={data.highRiskTransactions ? 'bad' : undefined}
          />
        </div>
      </section>

      <section className="card admin-dashboard-chart-card">
        <div className="admin-dashboard-card-heading">
          <div>
            <h2>Transaction volume</h2>
            <p>Daily transaction amounts over the last 14 days.</p>
          </div>

          <span className="admin-dashboard-chart-legend">
            <span />
            Transaction amount
          </span>
        </div>

        {series.length > 0 ? (
          <div className="admin-dashboard-chart">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart
                data={series}
                margin={{ top: 12, right: 12, left: 0, bottom: 0 }}
              >
                <defs>
                  <linearGradient
                    id="adminVolumeGradient"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop
                      offset="0%"
                      stopColor="#4169d8"
                      stopOpacity={0.22}
                    />
                    <stop
                      offset="95%"
                      stopColor="#4169d8"
                      stopOpacity={0.01}
                    />
                  </linearGradient>
                </defs>

                <CartesianGrid
                  strokeDasharray="4 4"
                  stroke="#e8edf5"
                  vertical={false}
                />

                <XAxis
                  dataKey="day"
                  tick={{ fontSize: 11, fill: '#8190a5' }}
                  axisLine={false}
                  tickLine={false}
                  tickMargin={12}
                />

                <YAxis
                  tick={{ fontSize: 11, fill: '#8190a5' }}
                  tickFormatter={(value) => compactMoney(value)}
                  axisLine={false}
                  tickLine={false}
                  width={65}
                />

                <Tooltip
                  formatter={(value, name) => [
                    name === 'Volume' ? money(value) : value,
                    name,
                  ]}
                  contentStyle={{
                    border: '1px solid #e2e8f3',
                    borderRadius: '12px',
                    boxShadow: '0 8px 24px rgba(30, 58, 138, 0.09)',
                    fontSize: '12px',
                  }}
                />

                <Area
                  type="monotone"
                  dataKey="amount"
                  name="Volume"
                  stroke="#4169d8"
                  strokeWidth={2.5}
                  fill="url(#adminVolumeGradient)"
                  activeDot={{
                    r: 5,
                    strokeWidth: 2,
                    stroke: '#ffffff',
                    fill: '#4169d8',
                  }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="admin-dashboard-chart-empty">
            <EmptyState title="No chart data available">
              Transaction volume will appear here when analytics data is available.
            </EmptyState>
          </div>
        )}
      </section>

      <section className="card admin-dashboard-table-card">
        <div className="admin-dashboard-card-heading">
          <div>
            <h2>Suspicious and blocked transactions</h2>
            <p>Transactions flagged by the platform's risk controls.</p>
          </div>

          <span className="admin-dashboard-risk-label">
            <span />
            Risk monitoring
          </span>
        </div>

        {data.suspiciousTransactions.length === 0 ? (
          <div className="admin-dashboard-empty">
            <EmptyState title="Nothing flagged">
              High-risk and blocked transactions will appear here when available.
            </EmptyState>
          </div>
        ) : (
          <div className="admin-dashboard-table-wrap">
            <TransactionTable rows={data.suspiciousTransactions} />
          </div>
        )}
      </section>

      <section className="card admin-dashboard-table-card">
        <div className="admin-dashboard-card-heading">
          <div>
            <h2>Recent transactions</h2>
            <p>Latest transactions returned by the platform analytics service.</p>
          </div>
        </div>

        {data.recentTransactions.length === 0 ? (
          <div className="admin-dashboard-empty">
            <EmptyState title="No transactions yet">
              Recent transaction activity will appear here.
            </EmptyState>
          </div>
        ) : (
          <div className="admin-dashboard-table-wrap">
            <TransactionTable rows={data.recentTransactions} />
          </div>
        )}
      </section>
    </div>
  );
}