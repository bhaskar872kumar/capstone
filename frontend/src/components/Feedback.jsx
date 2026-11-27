export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="spinner" role="status" aria-live="polite">
      <span className="spinner-dot" aria-hidden="true" />
      {label}
    </div>
  );
}

export function Alert({ type = 'error', title, children }) {
  if (!children && !title) return null;
  return (
    <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      {title && <strong className="alert-title">{title}</strong>}
      {children && <div>{children}</div>}
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {children && <div className="empty-body">{children}</div>}
    </div>
  );
}

export function StatusBadge({ value }) {
  if (!value) return <span>—</span>;
  const tone =
    {
      PAID: 'good',
      FULLY_PAID: 'good',
      ACTIVE: 'info',
      PENDING: 'neutral',
      PARTIALLY_PAID: 'warn',
      OVERDUE: 'bad',
      HIGH: 'good',
      MEDIUM: 'warn',
      LOW: 'neutral',
      AI: 'info',
      FALLBACK: 'warn',
    }[value] ?? 'neutral';
  const label = value === 'FALLBACK' ? 'Rule-based' : value.replaceAll('_', ' ').toLowerCase();
  return <span className={`badge badge-${tone}`}>{label}</span>;
}
