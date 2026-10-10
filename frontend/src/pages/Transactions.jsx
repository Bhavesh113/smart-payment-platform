import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import Pagination from '../components/Pagination';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';

const STATUSES = [
  'PENDING',
  'PROCESSING',
  'SUCCESS',
  'FAILED',
  'CANCELLED',
  'REVERSED',
];

export function useDebounced(value, ms = 350) {
  const [v, setV] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setV(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);

  return v;
}

export default function Transactions() {
  const [filters, setFilters] = useState({
    search: '',
    status: '',
    from: '',
    to: '',
  });

  const [sort, setSort] = useState({
    sortBy: 'createdAt',
    direction: 'desc',
  });

  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const search = useDebounced(filters.search);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const params = {
        page,
        size: 10,
        ...sort,
      };

      if (search.trim()) params.search = search.trim();
      if (filters.status) params.status = filters.status;
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;

      const { data } = await api.get('/api/transactions', { params });
      setResult(data.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [
    page,
    sort,
    search,
    filters.status,
    filters.from,
    filters.to,
  ]);

  useEffect(() => {
    load();
  }, [load]);

  const change = (key) => (e) => {
    const value = e.target.value;

    setPage(0);
    setFilters((current) => ({
      ...current,
      [key]: value,
    }));
  };

  const toggleSort = (field) => {
    setPage(0);

    setSort((current) =>
      current.sortBy === field
        ? {
            sortBy: field,
            direction: current.direction === 'asc' ? 'desc' : 'asc',
          }
        : {
            sortBy: field,
            direction: 'desc',
          }
    );
  };

  const clear = () => {
    setPage(0);
    setFilters({
      search: '',
      status: '',
      from: '',
      to: '',
    });
  };

  const hasFilters = Boolean(
    filters.search || filters.status || filters.from || filters.to
  );

  return (
    <div className="transactions-page">
      <header className="page-head transactions-heading">
        <div>
          <span className="transactions-eyebrow">
            <span className="transactions-eyebrow-dot" />
            ACTIVITY & HISTORY
          </span>

          <h1>Transactions</h1>

          <p>
            View, search, and manage your SmartPay transaction history.
          </p>
        </div>

        {result && (
          <div className="transactions-count">
            <span className="transactions-count-label">
              Total transactions
            </span>
            <strong>{result.totalElements ?? 0}</strong>
          </div>
        )}
      </header>

      <section className="card transactions-card">
        <div className="transactions-card-header">
          <div>
            <h2>Transaction history</h2>
            <p>Find a payment using its details, status, or date.</p>
          </div>

          {hasFilters && (
            <button
              type="button"
              className="btn btn-ghost btn-sm transactions-clear-top"
              onClick={clear}
            >
              Clear filters
            </button>
          )}
        </div>

        <div className="transactions-filters">
          <label className="transactions-search-field">
            <span>Search transactions</span>
            <div className="transactions-input-wrap">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <circle
                  cx="10.8"
                  cy="10.8"
                  r="6.8"
                  stroke="currentColor"
                  strokeWidth="1.8"
                />
                <path
                  d="M16 16L21 21"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                />
              </svg>

              <input
                type="search"
                placeholder="Reference, name, email, note"
                value={filters.search}
                onChange={change('search')}
              />
            </div>
          </label>

          <label>
            <span>Status</span>
            <select
              value={filters.status}
              onChange={change('status')}
            >
              <option value="">All statuses</option>
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status.charAt(0) + status.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>From date</span>
            <input
              type="date"
              value={filters.from}
              max={filters.to || undefined}
              onChange={change('from')}
            />
          </label>

          <label>
            <span>To date</span>
            <input
              type="date"
              value={filters.to}
              min={filters.from || undefined}
              onChange={change('to')}
            />
          </label>
        </div>

        {hasFilters && (
          <div className="transactions-active-filters">
            <span>
              {loading ? 'Updating results…' : 'Filters applied'}
            </span>

            <button type="button" onClick={clear}>
              Reset all
              <span aria-hidden="true"> ×</span>
            </button>
          </div>
        )}

        <div className="transactions-content">
          <ErrorBox message={error} onRetry={load} />

          {loading && !result && (
            <Spinner label="Loading transactions" />
          )}

          {result && result.content.length === 0 && !loading && (
            <div className="transactions-empty">
              <div className="transactions-empty-icon">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <rect
                    x="3.5"
                    y="5"
                    width="17"
                    height="14"
                    rx="2.5"
                    stroke="currentColor"
                    strokeWidth="1.6"
                  />
                  <path
                    d="M7 9H17M7 13H13"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinecap="round"
                  />
                </svg>
              </div>

              <EmptyState
                title={
                  hasFilters
                    ? 'No matching transactions'
                    : 'No transactions yet'
                }
              >
                {hasFilters
                  ? 'Try changing your search, selecting another status, or widening the date range.'
                  : 'Your payments and wallet activity will appear here once you start using SmartPay.'}
              </EmptyState>

              {hasFilters && (
                <button
                  type="button"
                  className="btn btn-ghost transactions-empty-button"
                  onClick={clear}
                >
                  Clear all filters
                </button>
              )}
            </div>
          )}

          {result && result.content.length > 0 && (
            <div className={loading ? 'dim' : ''}>
              <div className="transactions-results-bar">
                <span>
                  Showing{' '}
                  <strong>{result.content.length}</strong>{' '}
                  transaction{result.content.length === 1 ? '' : 's'}
                </span>

                <span className="transactions-sort-caption">
                  Sorted by{' '}
                  {sort.sortBy === 'createdAt'
                    ? 'date'
                    : sort.sortBy}
                  {' · '}
                  {sort.direction === 'desc'
                    ? 'Newest first'
                    : 'Oldest first'}
                </span>
              </div>

              <div className="transactions-table-wrap">
                <TransactionTable
                  rows={result.content}
                  sort={sort}
                  onSort={toggleSort}
                />
              </div>

              <div className="transactions-pagination">
                <Pagination
                  page={result.page}
                  totalPages={result.totalPages}
                  totalElements={result.totalElements}
                  onChange={setPage}
                />
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}