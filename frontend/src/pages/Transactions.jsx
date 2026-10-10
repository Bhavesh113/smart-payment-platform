import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import Pagination from '../components/Pagination';
import TransactionTable from '../components/TransactionTable';
import { EmptyState, ErrorBox, Spinner } from '../components/States';

const STATUSES = ['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REVERSED'];

export function useDebounced(value, ms = 350) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

export default function Transactions() {
  const [filters, setFilters] = useState({ search: '', status: '', from: '', to: '' });
  const [sort, setSort] = useState({ sortBy: 'createdAt', direction: 'desc' });
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const search = useDebounced(filters.search);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: 10, ...sort };
      if (search) params.search = search;
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
  }, [page, sort, search, filters.status, filters.from, filters.to]);

  useEffect(() => { load(); }, [load]);

  const change = (key) => (e) => { setPage(0); setFilters({ ...filters, [key]: e.target.value }); };
  const toggleSort = (field) => {
    setPage(0);
    setSort((s) => (s.sortBy === field ? { sortBy: field, direction: s.direction === 'asc' ? 'desc' : 'asc' } : { sortBy: field, direction: 'desc' }));
  };
  const clear = () => { setPage(0); setFilters({ search: '', status: '', from: '', to: '' }); };
  const hasFilters = filters.search || filters.status || filters.from || filters.to;

  return (
    <>
      <header className="page-head"><h1>Transactions</h1></header>
      <section className="card">
        <div className="filters">
          <label>Search
            <input type="search" placeholder="Reference, name, email, note" value={filters.search} onChange={change('search')} />
          </label>
          <label>Status
            <select value={filters.status} onChange={change('status')}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
            </select>
          </label>
          <label>From<input type="date" value={filters.from} max={filters.to || undefined} onChange={change('from')} /></label>
          <label>To<input type="date" value={filters.to} min={filters.from || undefined} onChange={change('to')} /></label>
          {hasFilters && <button className="btn btn-ghost btn-sm" onClick={clear}>Clear filters</button>}
        </div>
        <ErrorBox message={error} onRetry={load} />
        {loading && !result ? <Spinner label="Loading transactions" /> : null}
        {result && result.content.length === 0 && (
          <EmptyState title={hasFilters ? 'No transactions match these filters' : 'No transactions yet'}>
            {hasFilters ? 'Try widening the date range or clearing the search.' : 'Add money or send a payment to get started.'}
          </EmptyState>
        )}
        {result && result.content.length > 0 && (
          <div className={loading ? 'dim' : ''}>
            <TransactionTable rows={result.content} sort={sort} onSort={toggleSort} />
            <Pagination page={result.page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
          </div>
        )}
      </section>
    </>
  );
}
