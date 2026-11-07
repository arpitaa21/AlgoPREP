import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { testApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import { formatDateTime } from '../utils/format.js';

export default function Tests() {
  const navigate = useNavigate();
  const [tests, setTests] = useState(null);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    testApi.list().then(setTests).catch((e) => setError(errorMessage(e)));
    testApi.history().then(setHistory).catch(() => {});
  }, []);

  return (
    <>
      <h2 className="mb-3">Timed tests</h2>
      <ErrorAlert message={error} />
      {!tests ? <Loading /> : tests.length === 0 ? <p className="text-muted">No tests available yet.</p> : (
        <div className="row g-3 mb-4">
          {tests.map((t) => (
            <div className="col-md-4" key={t.id}>
              <div className="card h-100"><div className="card-body d-flex flex-column">
                <h5>{t.title}</h5>
                <p className="text-muted small flex-grow-1">{t.description}</p>
                <div className="small mb-2">{t.questionCount} questions &middot; {t.durationMinutes} minutes</div>
                <button className="btn btn-primary" onClick={() => navigate(`/tests/${t.id}/take`)}>Start / resume</button>
              </div></div>
            </div>
          ))}
        </div>
      )}

      <h5>Your attempts</h5>
      {history.length === 0 ? <p className="text-muted small">No attempts yet.</p> : (
        <table className="table table-sm">
          <thead><tr><th>Test</th><th>Started</th><th>Status</th><th>Score</th><th /></tr></thead>
          <tbody>{history.map((a) => (
            <tr key={a.id}>
              <td>{a.testTitle}</td><td>{formatDateTime(a.startedAt)}</td><td>{a.status}</td>
              <td>{a.status === 'SUBMITTED' ? `${a.score}/${a.totalQuestions} (${a.percentage}%)` : '-'}</td>
              <td>{a.status === 'SUBMITTED' && <Link to={`/attempts/${a.id}`}>Review</Link>}</td>
            </tr>
          ))}</tbody>
        </table>
      )}
    </>
  );
}
