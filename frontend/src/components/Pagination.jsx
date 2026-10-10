export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalElements) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <span className="muted-text">
        Page {page + 1} of {Math.max(totalPages, 1)} · {totalElements} transactions
      </span>
      <div>
        <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
          Previous
        </button>
        <button
          className="btn btn-ghost btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Next
        </button>
      </div>
    </nav>
  );
}
