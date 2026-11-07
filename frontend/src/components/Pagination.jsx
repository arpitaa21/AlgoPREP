// page is 0-based (like the backend); the UI shows it 1-based.
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="d-flex justify-content-center align-items-center gap-3 my-3">
      <button className="btn btn-outline-secondary btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="small text-muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn btn-outline-secondary btn-sm"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}
