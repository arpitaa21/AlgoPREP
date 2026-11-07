import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { testApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';
import { formatClock } from '../utils/format.js';

export default function TakeTest() {
  const { testId } = useParams();
  const navigate = useNavigate();
  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({});          // { questionId: optionId }
  const [index, setIndex] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(null);   // null = timer not started yet
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const submittedRef = useRef(false);                  // prevents a double submit (manual click + timer reaching 0)

  // 1. start (or resume) the attempt. The SERVER decides how many seconds remain.
  useEffect(() => {
    testApi.start(testId)
      .then((a) => {
        setAttempt(a);
        // answers are kept in sessionStorage so a page refresh does not lose them
        try {
          const saved = JSON.parse(sessionStorage.getItem(`answers-${a.attemptId}`) || '{}');
          setAnswers(saved);
        } catch { /* ignore corrupted storage */ }
      })
      .catch((e) => setError(errorMessage(e)));
  }, [testId]);

  useEffect(() => {
    if (attempt) sessionStorage.setItem(`answers-${attempt.attemptId}`, JSON.stringify(answers));
  }, [answers, attempt]);

  const submit = useCallback(async () => {
    if (!attempt || submittedRef.current) return;
    submittedRef.current = true;
    setSubmitting(true);
    setError('');
    try {
      const payload = attempt.questions.map((q) => ({ questionId: q.id, optionId: answers[q.id] ?? null }));
      await testApi.submit(attempt.attemptId, payload);
      sessionStorage.removeItem(`answers-${attempt.attemptId}`);
      navigate(`/attempts/${attempt.attemptId}`, { replace: true });
    } catch (e) {
      submittedRef.current = false;
      setSubmitting(false);
      setError(errorMessage(e));
    }
  }, [attempt, answers, navigate]);

  // 2. countdown. We count down against a fixed END time (not "subtract 1 each tick"),
  //    because browsers slow timers in background tabs and a naive counter would drift.
  useEffect(() => {
    if (!attempt) return undefined;
    const endsAt = Date.now() + attempt.secondsRemaining * 1000;
    const tick = () => {
      const left = Math.max(0, Math.round((endsAt - Date.now()) / 1000));
      setSecondsLeft(left);                            // auto-submit is handled by the effect below
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [attempt]);

  // When the countdown reaches exactly 0, submit whatever has been answered so far.
  // This effect re-runs with the LATEST `submit` (and therefore the latest answers); calling submit()
  // from inside the interval callback would use stale answers captured when the timer started.
  useEffect(() => {
    if (secondsLeft === 0) submit();
  }, [secondsLeft, submit]);

  if (!attempt) return error ? <ErrorAlert message={error} /> : <Loading text="Starting test..." />;

  const q = attempt.questions[index];
  const answered = Object.keys(answers).length;
  const low = secondsLeft !== null && secondsLeft <= 60;

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3 sticky-top bg-white py-2 border-bottom">
        <div>
          <h4 className="mb-0">{attempt.title}</h4>
          <span className="small text-muted">{answered} / {attempt.questions.length} answered</span>
        </div>
        <div className={`fs-3 fw-bold font-monospace ${low ? 'text-danger' : ''}`} aria-live="polite">
          {formatClock(secondsLeft ?? 0)}
        </div>
      </div>

      <ErrorAlert message={error} />

      <div className="card mb-3"><div className="card-body">
        <div className="small text-muted mb-1">Question {index + 1} of {attempt.questions.length} &middot; {q.topicName}</div>
        <h5>{q.title}</h5>
        {q.options.map((o) => (
          <div className="form-check my-2" key={o.id}>
            <input className="form-check-input" type="radio" name={`q${q.id}`} id={`opt${o.id}`}
                   checked={answers[q.id] === o.id} onChange={() => setAnswers({ ...answers, [q.id]: o.id })} />
            <label className="form-check-label" htmlFor={`opt${o.id}`}>{o.text}</label>
          </div>
        ))}
        {answers[q.id] !== undefined && (
          <button className="btn btn-link btn-sm p-0" onClick={() => {
            const { [q.id]: _removed, ...rest } = answers;   // clear selection = skip
            setAnswers(rest);
          }}>Clear answer</button>
        )}
      </div></div>

      <div className="d-flex flex-wrap gap-1 mb-3">
        {attempt.questions.map((x, i) => (
          <button key={x.id} onClick={() => setIndex(i)}
                  className={`btn btn-sm ${i === index ? 'btn-primary' : answers[x.id] !== undefined ? 'btn-success' : 'btn-outline-secondary'}`}>
            {i + 1}
          </button>
        ))}
      </div>

      <div className="d-flex justify-content-between">
        <div className="d-flex gap-2">
          <button className="btn btn-outline-secondary" disabled={index === 0} onClick={() => setIndex(index - 1)}>Previous</button>
          <button className="btn btn-outline-secondary" disabled={index === attempt.questions.length - 1} onClick={() => setIndex(index + 1)}>Next</button>
        </div>
        <button className="btn btn-success" disabled={submitting}
                onClick={() => { if (window.confirm(`Submit now? You answered ${answered} of ${attempt.questions.length}.`)) submit(); }}>
          {submitting ? 'Submitting...' : 'Submit test'}
        </button>
      </div>
    </>
  );
}
