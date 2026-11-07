export default function Loading({ text = 'Loading...' }) {
  return (
    <div className="text-center text-muted py-5">
      <div className="spinner-border spinner-border-sm me-2" role="status" />
      {text}
    </div>
  );
}
