import { Link } from 'react-router-dom';
import { RiskBadge, StatusBadge } from './Badges';
import { dateTime, money, titleCase } from '../utils/format';

/** Sortable columns map to the backend's whitelist (createdAt, amount, status, riskLevel). */
function SortHeader({ label, field, sort, onSort }) {
  if (!onSort) return <th>{label}</th>;
  const active = sort?.sortBy === field;
  const arrow = active ? (sort.direction === 'asc' ? ' ↑' : ' ↓') : '';
  return (
    <th aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button className="th-sort" onClick={() => onSort(field)}>
        {label}
        {arrow}
      </button>
    </th>
  );
}

export default function TransactionTable({ rows, sort, onSort, compact = false }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Transaction ID</th>
            <SortHeader label="Date" field="createdAt" sort={sort} onSort={onSort} />
            {!compact && <th>Sender</th>}
            {!compact && <th>Receiver</th>}
            <SortHeader label="Amount" field="amount" sort={sort} onSort={onSort} />
            <th>Type</th>
            <SortHeader label="Status" field="status" sort={sort} onSort={onSort} />
            <SortHeader label="Risk level" field="riskLevel" sort={sort} onSort={onSort} />
          </tr>
        </thead>
        <tbody>
          {rows.map((t) => (
            <tr key={t.id}>
              <td data-label="Transaction ID">
                <Link className="ref" to={`/transactions/${t.id}`}>
                  {t.transactionReference}
                </Link>
              </td>
              <td data-label="Date">{dateTime(t.createdAt)}</td>
              {!compact && (
                <td data-label="Sender">
                  {t.senderName}
                  <small>{t.senderEmail}</small>
                </td>
              )}
              {!compact && (
                <td data-label="Receiver">
                  {t.transactionType === 'ADD_MONEY' ? <em>Own wallet</em> : t.receiverName}
                  {t.transactionType !== 'ADD_MONEY' && <small>{t.receiverEmail}</small>}
                </td>
              )}
              <td data-label="Amount" className={`num ${t.direction === 'DEBIT' ? 'debit' : t.direction === 'CREDIT' ? 'credit' : ''}`}>
                {t.direction === 'DEBIT' ? '− ' : t.direction === 'CREDIT' ? '+ ' : ''}
                {money(t.amount, t.currency)}
              </td>
              <td data-label="Type">{t.transactionType === 'ADD_MONEY' ? 'Top-up' : titleCase(t.transactionType)}</td>
              <td data-label="Status">
                <StatusBadge status={t.status} />
              </td>
              <td data-label="Risk level">
                <RiskBadge level={t.riskLevel} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
