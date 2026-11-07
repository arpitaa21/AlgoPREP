const COLORS = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' };

export default function DifficultyBadge({ level }) {
  return <span className={`badge text-bg-${COLORS[level] || 'secondary'}`}>{level}</span>;
}
