import { useEffect, useState } from 'react';
import { adminApi } from '../../services/endpoints.js';
import { errorMessage } from '../../services/api.js';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import Loading from '../../components/Loading.jsx';

export default function AdminTests() {
  const [tests, setTests] = useState(null);
  const [mcqs, setMcqs] = useState([]);
  const [form, setForm] = useState({ title: '', description: '', durationMinutes: 15, active: true });
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState('');

  const load = () => adminApi.listTests().then(setTests).catch((e) => setError(errorMessage(e)));
  useEffect(() => {
    load();
    adminApi.listQuestions({ type: 'MCQ', size: 50 }).then((p) => setMcqs(p.content)).catch(() => {});
  }, []);

  const toggle = (id) => setSelected(selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id]);

  const create = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await adminApi.createTest({ ...form, durationMinutes: Number(form.durationMinutes), questionIds: selected });
      setForm({ title: '', description: '', durationMinutes: 15, active: true });
      setSelected([]);
      load();
    } catch (err) { setError(errorMessage(err)); }
  };

  const toggleActive = async (t) => {
    try {
      await adminApi.updateTest(t.id, { title: t.title, description: t.description, durationMinutes: t.durationMinutes,
        active: !t.active, questionIds: t.questionIds });
      load();
    } catch (err) { setError(errorMessage(err)); }
  };

  const remove = async (t) => {
    if (!window.confirm(`Delete test "${t.title}"?`)) return;
    try { await adminApi.deleteTest(t.id); load(); } catch (err) { setError(errorMessage(err)); }
  };

  return (
    <>
      <h2 className="mb-3">Manage tests</h2>
      <ErrorAlert message={error} />

      <form className="card card-body mb-4" onSubmit={create}>
        <h5>Create test</h5>
        <div className="row g-2">
          <div className="col-md-5"><input className="form-control" placeholder="Title" required value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} /></div>
          <div className="col-md-5"><input className="form-control" placeholder="Description" value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
          <div className="col-md-2"><input className="form-control" type="number" min="1" max="300" required value={form.durationMinutes}
            onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} title="Duration in minutes" /></div>
        </div>
        <div className="mt-3 small text-muted">Pick MCQ questions ({selected.length} selected):</div>
        <div className="border rounded p-2" style={{ maxHeight: 220, overflow: 'auto' }}>
          {mcqs.map((q) => (
            <div className="form-check" key={q.id}>
              <input className="form-check-input" type="checkbox" id={`mq${q.id}`} checked={selected.includes(q.id)} onChange={() => toggle(q.id)} />
              <label className="form-check-label" htmlFor={`mq${q.id}`}>{q.title} <span className="text-muted small">({q.topicName})</span></label>
            </div>
          ))}
        </div>
        <button className="btn btn-primary mt-3 align-self-start" disabled={selected.length === 0}>Create test</button>
      </form>

      {!tests ? <Loading /> : (
        <table className="table align-middle">
          <thead><tr><th>Title</th><th>Minutes</th><th>Questions</th><th>Status</th><th /></tr></thead>
          <tbody>{tests.map((t) => (
            <tr key={t.id}>
              <td>{t.title}</td><td>{t.durationMinutes}</td><td>{t.questionIds.length}</td>
              <td>{t.active ? <span className="badge text-bg-success">Active</span> : <span className="badge text-bg-secondary">Hidden</span>}</td>
              <td className="text-end text-nowrap">
                <button className="btn btn-sm btn-outline-secondary me-2" onClick={() => toggleActive(t)}>{t.active ? 'Hide' : 'Publish'}</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(t)}>Delete</button>
              </td>
            </tr>
          ))}</tbody>
        </table>
      )}
    </>
  );
}
