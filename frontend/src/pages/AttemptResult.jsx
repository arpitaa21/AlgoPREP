import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { testApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function AttemptResult() {
  const { attemptId } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    testApi.attempt(attemptId).then(setData).catch((e) => setError(errorMessage(e)));
  }, [attemptId]);

  if (!data) return error ? <ErrorAlert message={error} /> : <Loading />;
  const { summary, review } = data;

  // group wrong answers by topic to show where to focus
  const wrongByTopic = review.filter((r) => !r.correct).reduce((acc, r) => {
    acc[r.topicName] = (acc[r.topicName] || 0) + 1;
    return acc;
  }, {});

  return (
    <>
      <h2>{summary.testTitle}</h2>
      {summary.status !== 'SUBMITTED' ? (
        <div className="alert alert-warning">This attempt was not submitted ({summary.status}), so there is no score.</div>
      ) : (
        <>
          <div className="alert alert-info fs-5">
            Score: <strong>{summary.score} / {summary.totalQuestions}</strong> ({summary.percentage}%)
          </div>
          {Object.keys(wrongByTopic).length > 0 && (
            <p>Focus on: {Object.entries(wrongByTopic).map(([t, n]) => `${t} (${n} missed)`).join(', ')}.{' '}
              <Link to="/">See recommendations on your dashboard</Link></p>
          )}
          {review.map((r, i) => (
            <div key={r.questionId} className={`card mb-2 border-${r.correct ? 'success' : 'danger'}`}>
              <div className="card-body py-2">
                <div className="d-flex justify-content-between">
                  <strong>{i + 1}. {r.title}</strong>
                  <span className={r.correct ? 'text-success' : 'text-danger'}>
                    {r.correct ? 'Correct' : r.selectedOptionId ? 'Wrong' : 'Skipped'}
                  </span>
                </div>
                <div className="small text-muted">{r.topicName}</div>
                {r.explanation && <div className="small mt-1">{r.explanation}</div>}
              </div>
            </div>
          ))}
        </>
      )}
      <Link className="btn btn-primary mt-3" to="/tests">Back to tests</Link>
    </>
  );
}
