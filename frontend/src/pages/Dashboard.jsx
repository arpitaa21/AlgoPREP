import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { recommendationApi, statsApi, testApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import DifficultyBadge from '../components/DifficultyBadge.jsx';

function ProgressBar({ value, label }) {
  return (
    <div className="progress" role="progressbar" aria-label={label} style={{ height: 14 }}>
      <div className="progress-bar" style={{ width: `${Math.min(100, value)}%` }} />
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [weak, setWeak] = useState([]);
  const [recs, setRecs] = useState([]);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    // Four independent requests run in parallel.
    Promise.all([statsApi.me(), recommendationApi.weakTopics(), recommendationApi.list(6), testApi.history()])
      .then(([s, w, r, h]) => {
        setStats(s);
        setWeak(w);
        setRecs(r);
        setHistory(h.filter((a) => a.status === 'SUBMITTED').slice(0, 5));
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <ErrorAlert message={error} />;
  if (!stats) return <Loading />;

  const pct = stats.totalDsaQuestions ? Math.round((stats.solvedCount * 100) / stats.totalDsaQuestions) : 0;
  const days = Object.entries(stats.submissionsLast30Days);
  const maxDay = Math.max(1, ...days.map(([, n]) => n));

  return (
    <>
      <h2 className="mb-3">Welcome, {user.name}</h2>

      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3"><div className="card text-center"><div className="card-body">
          <div className="fs-2 fw-bold">{stats.solvedCount}<span className="fs-6 text-muted"> / {stats.totalDsaQuestions}</span></div>
          <div className="small text-muted">DSA solved ({pct}%)</div></div></div></div>
        <div className="col-6 col-md-3"><div className="card text-center"><div className="card-body">
          <div className="fs-2 fw-bold">{stats.currentStreakDays}</div>
          <div className="small text-muted">Day streak</div></div></div></div>
        <div className="col-6 col-md-3"><div className="card text-center"><div className="card-body">
          <div className="fs-2 fw-bold">{stats.testsTaken}</div>
          <div className="small text-muted">Tests taken</div></div></div></div>
        <div className="col-6 col-md-3"><div className="card text-center"><div className="card-body">
          <div className="fs-2 fw-bold">{stats.averageTestPercentage}%</div>
          <div className="small text-muted">Average test score</div></div></div></div>
      </div>

      <div className="row g-4">
        <div className="col-lg-6">
          <h5>Progress by difficulty</h5>
          {['EASY', 'MEDIUM', 'HARD'].map((d) => (
            <div className="mb-2" key={d}>
              <div className="d-flex justify-content-between small">
                <DifficultyBadge level={d} />
                <span>{stats.solvedByDifficulty[d]} / {stats.totalByDifficulty[d]}</span>
              </div>
              <ProgressBar label={d} value={stats.totalByDifficulty[d] ? (stats.solvedByDifficulty[d] * 100) / stats.totalByDifficulty[d] : 0} />
            </div>
          ))}

          <h5 className="mt-4">Progress by topic</h5>
          {stats.topicProgress.filter((t) => t.total > 0).map((t) => (
            <div className="mb-2" key={t.topicId}>
              <div className="d-flex justify-content-between small">
                <span>{t.topicName}</span><span>{t.solved} / {t.total}</span>
              </div>
              <ProgressBar label={t.topicName} value={t.percentage} />
            </div>
          ))}

          <h5 className="mt-4">Submissions, last 30 days</h5>
          <div className="d-flex align-items-end gap-1" style={{ height: 70 }} aria-label="submissions per day">
            {days.map(([day, n]) => (
              <div key={day} title={`${day}: ${n}`} className={n ? 'bg-success' : 'bg-secondary-subtle'}
                   style={{ flex: 1, height: `${Math.max(6, (n * 100) / maxDay)}%` }} />
            ))}
          </div>
        </div>

        <div className="col-lg-6">
          <h5>Weak topics</h5>
          {weak.length === 0 ? (
            <p className="text-muted small">
              Nothing flagged yet. A topic is flagged after at least 3 answered MCQs with accuracy below 50%.{' '}
              <Link to="/tests">Take a test</Link> to get personalised suggestions.
            </p>
          ) : (
            <ul className="list-group mb-3">
              {weak.map((w) => (
                <li className="list-group-item d-flex justify-content-between" key={w.topicId}>
                  <span>{w.topicName}</span>
                  <span className="text-danger small">{w.correct}/{w.attempted} correct ({Math.round(w.accuracy * 100)}%)</span>
                </li>
              ))}
            </ul>
          )}

          <h5>Recommended practice</h5>
          {recs.length === 0 ? <p className="text-muted small">No recommendations right now.</p> : (
            <ul className="list-group mb-3">
              {recs.map((r) => (
                <li className="list-group-item" key={r.questionId}>
                  <Link to={`/questions/${r.questionId}`}>{r.title}</Link>{' '}
                  <DifficultyBadge level={r.difficulty} />
                  <div className="small text-muted">{r.reason}</div>
                </li>
              ))}
            </ul>
          )}

          <h5>Recent tests</h5>
          {history.length === 0 ? <p className="text-muted small">No tests taken yet.</p> : (
            <ul className="list-group">
              {history.map((a) => (
                <li className="list-group-item d-flex justify-content-between" key={a.id}>
                  <Link to={`/attempts/${a.id}`}>{a.testTitle}</Link>
                  <span>{a.score}/{a.totalQuestions} ({a.percentage}%)</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}
