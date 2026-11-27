const rupees = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—';
  return rupees.format(Number(value));
}

/** "2026-11-20" → "20-Nov-2026" */
export function formatDate(iso) {
  if (!iso) return '—';
  const [year, month, day] = String(iso).split('-');
  if (!year || !month || !day) return iso;
  return `${day}-${MONTHS[Number(month) - 1]}-${year}`;
}

/** "HALF_YEARLY" → "Half yearly" */
export function formatLabel(value) {
  if (!value) return '—';
  const text = String(value).replaceAll('_', ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Today's date in the browser's time zone as yyyy-mm-dd. */
export function todayIso() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}
