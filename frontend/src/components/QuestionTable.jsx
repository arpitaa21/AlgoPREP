import { Link } from 'react-router-dom';
import DifficultyBadge from './DifficultyBadge.jsx';

/** Shared table of question summaries used by the browser, bookmarks and revision pages. */
export default function QuestionTable({ questions, onToggleBookmark, onRemove, removeLabel = 'Remove' }) {
  if (questions.length === 0) {
    return <p className="text-muted">No questions to show.</p>;
  }
  return (
    <div className="table-responsive">
      <table className="table table-hover align-middle">
        <thead>
          <tr>
            <th style={{ width: 40 }}>Done</th>
            <th>Title</th>
            <th>Topic</th>
            <th>Level</th>
            <th>Type</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {questions.map((q) => (
            <tr key={q.id}>
              <td>{q.solved ? <span className="text-success fw-bold">&#10003;</span> : ''}</td>
              <td><Link to={`/questions/${q.id}`}>{q.title}</Link></td>
              <td>{q.topicName}</td>
              <td><DifficultyBadge level={q.difficulty} /></td>
              <td><span className="badge text-bg-light border">{q.type}</span></td>
              <td className="text-end text-nowrap">
                {onToggleBookmark && (
                  <button
                    className={`btn btn-sm ${q.bookmarked ? 'btn-warning' : 'btn-outline-warning'}`}
                    onClick={() => onToggleBookmark(q)}
                    title="Bookmark"
                  >
                    {q.bookmarked ? '★' : '☆'}
                  </button>
                )}
                {onRemove && (
                  <button className="btn btn-sm btn-outline-danger ms-2" onClick={() => onRemove(q)}>
                    {removeLabel}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
