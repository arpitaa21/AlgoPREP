export function formatDateTime(iso) {
  if (!iso) return '-';
  return new Date(iso).toLocaleString();
}

export function formatClock(totalSeconds) {
  const s = Math.max(0, totalSeconds);
  const mm = String(Math.floor(s / 60)).padStart(2, '0');
  const ss = String(s % 60).padStart(2, '0');
  return `${mm}:${ss}`;
}
