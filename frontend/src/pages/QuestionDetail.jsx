import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { bookmarkApi, questionApi, revisionApi, solvedApi, submissionApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import DifficultyBadge from '../components/DifficultyBadge.jsx';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import { formatDateTime } from '../utils/format.js';

export default function QuestionDetail() {
  const { id } = useParams();
  const [q, setQ] = useState(null);
  const [lists, setLists] = useState([]);
  const [subs, setSubs] = useState([]);
  const [choice, setChoice] = useState(null);
  const [result, setResult] = useState(null);
  const [form, setForm] = useState({ language: 'Java', status: 'SOLVED', timeTakenMinutes: '', code: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = () => {
    questionApi.get(id).then(setQ).catch((e) => setError(errorMessage(e)));
  };

  useEffect(() => {
    setQ(null); setResult(null); setChoice(null); setNotice('');
    load();
    revisionApi.list().then(setLists).catch(() => {});
    submissionApi.list(id).then(setSubs).catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const run = async (fn, okMessage) => {
    setError(''); setNotice('');
    try {
      await fn();
      if (okMessage) setNotice(okMessage);
      load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  if (!q) return error ? <ErrorAlert message={error} /> : <Loading />;

  const submitPractice = () => run(async () => setResult(await questionApi.practice(q.id, choice)));
  const submitCode = (e) => {
    e.preventDefault();
    run(async () => {
      await submissionApi.record({
        questionId: q.id, language: form.language, code: form.code, status: form.status,
        timeTakenMinutes: form.timeTakenMinutes === '' ? null : Number(form.timeTakenMinutes),
      });
      setSubs(await submissionApi.list(q.id));
    }, 'Submission recorded');
  };

  return (
    <>
      <Link to="/questions" className="small">&larr; Back to questions</Link>
      <div className="d-flex align-items-center gap-2 mt-2 flex-wrap">
        <h2 className="mb-0">{q.title}</h2>
        <DifficultyBadge level={q.difficulty} />
        <span className="badge text-bg-light border">{q.type}</span>
        <span className="text-muted">{q.topicName}</span>
        {q.solved && <span className="badge text-bg-success">Solved</span>}
      </div>

      <ErrorAlert message={error} />
      {notice && <div className="alert alert-success py-2 mt-2">{notice}</div>}

      <p className="mt-3" style={{ whiteSpace: 'pre-wrap' }}>{q.description}</p>

      <div className="d-flex gap-2 flex-wrap mb-3">
        <button className={`btn btn-sm ${q.bookmarked ? 'btn-warning' : 'btn-outline-warning'}`}
                onClick={() => run(() => (q.bookmarked ? bookmarkApi.remove(q.id) : bookmarkApi.add(q.id)))}>
          {q.bookmarked ? '★ Bookmarked' : '☆ Bookmark'}
        </button>
        {q.type === 'DSA' && (
          <button className={`btn btn-sm ${q.solved ? 'btn-success' : 'btn-outline-success'}`}
                  onClick={() => run(() => (q.solved ? solvedApi.unmark(q.id) : solvedApi.mark(q.id)))}>
            {q.solved ? '✓ Solved (click to undo)' : 'Mark as solved'}
          </button>
        )}
        {q.externalLink && <a className="btn btn-sm btn-primary" href={q.externalLink} target="_blank" rel="noreferrer">Open problem &nearr;</a>}
        {lists.length > 0 && (
          <select className="form-select form-select-sm w-auto" defaultValue=""
                  onChange={(e) => { if (e.target.value) run(() => revisionApi.addQuestion(e.target.value, q.id), 'Added to revision list'); e.target.value = ''; }}>
            <option value="">+ Add to revision list</option>
            {lists.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
          </select>
        )}
      </div>

      {q.type === 'MCQ' && (
        <div className="card mb-3"><div className="card-body">
          <h6>Practise this question</h6>
          {q.options.map((o) => (
            <div className="form-check" key={o.id}>
              <input className="form-check-input" type="radio" name="opt" id={`o${o.id}`} disabled={!!result}
                     checked={choice === o.id} onChange={() => setChoice(o.id)} />
              <label className={`form-check-label ${result && o.id === result.correctOptionId ? 'text-success fw-bold' : ''}`} htmlFor={`o${o.id}`}>{o.text}</label>
            </div>
          ))}
          {!result ? (
            <button className="btn btn-primary btn-sm mt-3" disabled={choice === null} onClick={submitPractice}>Check answer</button>
          ) : (
            <div className={`alert mt-3 mb-0 ${result.correct ? 'alert-success' : 'alert-danger'}`}>
              <strong>{result.correct ? 'Correct!' : 'Not quite.'}</strong> {result.explanation}
            </div>
          )}
        </div></div>
      )}

      {q.type === 'DSA' && q.explanation && (
        <details className="mb-3"><summary>Show approach hint</summary><p className="mt-2">{q.explanation}</p></details>
      )}

      {q.type === 'DSA' && (
        <div className="card mb-3"><div className="card-body">
          <h6>Record a coding submission</h6>
          <form onSubmit={submitCode}>
            <div className="row g-2 mb-2">
              <div className="col-md-3">
                <select className="form-select" value={form.language} onChange={(e) => setForm({ ...form, language: e.target.value })}>
                  {['Java', 'Python', 'C++', 'JavaScript', 'C'].map((l) => <option key={l}>{l}</option>)}
                </select>
              </div>
              <div className="col-md-3">
                <select className="form-select" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <option value="SOLVED">Solved</option><option value="ATTEMPTED">Attempted</option>
                </select>
              </div>
              <div className="col-md-3">
                <input className="form-control" type="number" min="0" placeholder="Minutes taken" value={form.timeTakenMinutes}
                       onChange={(e) => setForm({ ...form, timeTakenMinutes: e.target.value })} />
              </div>
            </div>
            <textarea className="form-control mb-2" rows={5} placeholder="Paste your code (optional)" value={form.code}
                      onChange={(e) => setForm({ ...form, code: e.target.value })} />
            <button className="btn btn-success btn-sm">Save submission</button>
          </form>
          {subs.length > 0 && (
            <table className="table table-sm mt-3 mb-0">
              <thead><tr><th>When</th><th>Language</th><th>Status</th><th>Minutes</th></tr></thead>
              <tbody>{subs.map((s) => (
                <tr key={s.id}><td>{formatDateTime(s.submittedAt)}</td><td>{s.language}</td><td>{s.status}</td><td>{s.timeTakenMinutes ?? '-'}</td></tr>
              ))}</tbody>
            </table>
          )}
        </div></div>
      )}
    </>
  );
}
