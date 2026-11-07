import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { adminApi, topicApi } from '../../services/endpoints.js';
import { errorMessage } from '../../services/api.js';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import Loading from '../../components/Loading.jsx';

const EMPTY_OPTIONS = [
  { text: '', correct: true }, { text: '', correct: false }, { text: '', correct: false }, { text: '', correct: false },
];

export default function AdminQuestionForm() {
  const { id } = useParams();                       // present when editing
  const navigate = useNavigate();
  const [topics, setTopics] = useState([]);
  const [form, setForm] = useState({
    title: '', description: '', difficulty: 'EASY', type: 'DSA', topicId: '', externalLink: '', explanation: '',
  });
  const [options, setOptions] = useState(EMPTY_OPTIONS);
  const [loading, setLoading] = useState(!!id);
  const [error, setError] = useState('');

  useEffect(() => { topicApi.list().then(setTopics).catch((e) => setError(errorMessage(e))); }, []);

  useEffect(() => {
    if (!id) return;
    adminApi.getQuestion(id).then((q) => {
      setForm({
        title: q.title, description: q.description, difficulty: q.difficulty, type: q.type, topicId: String(q.topicId),
        externalLink: q.externalLink || '', explanation: q.explanation || '',
      });
      if (q.options.length) setOptions(q.options.map((o) => ({ text: o.text, correct: o.correct })));
    }).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, [id]);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });
  const setOption = (i, patch) => setOptions(options.map((o, idx) => (idx === i ? { ...o, ...patch } : o)));
  const markCorrect = (i) => setOptions(options.map((o, idx) => ({ ...o, correct: idx === i })));   // radio: exactly one

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    const body = {
      ...form,
      topicId: Number(form.topicId),
      externalLink: form.externalLink || null,
      explanation: form.explanation || null,
      options: form.type === 'MCQ' ? options.filter((o) => o.text.trim()) : [],
    };
    try {
      id ? await adminApi.updateQuestion(id, body) : await adminApi.createQuestion(body);
      navigate('/admin/questions');
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (loading) return <Loading />;

  return (
    <>
      <Link to="/admin/questions" className="small">&larr; Back</Link>
      <h2 className="mb-3 mt-2">{id ? 'Edit question' : 'New question'}</h2>
      <ErrorAlert message={error} />
      <form onSubmit={submit}>
        <div className="row g-3">
          <div className="col-md-8">
            <label className="form-label">Title</label>
            <input className="form-control" required maxLength={200} value={form.title} onChange={set('title')} />
          </div>
          <div className="col-md-4">
            <label className="form-label">Topic</label>
            <select className="form-select" required value={form.topicId} onChange={set('topicId')}>
              <option value="">Choose...</option>
              {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Type</label>
            <select className="form-select" value={form.type} onChange={set('type')}>
              <option value="DSA">DSA (coding)</option><option value="MCQ">MCQ</option>
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label">Difficulty</label>
            <select className="form-select" value={form.difficulty} onChange={set('difficulty')}>
              <option>EASY</option><option>MEDIUM</option><option>HARD</option>
            </select>
          </div>
          <div className="col-md-6">
            <label className="form-label">External link (optional)</label>
            <input className="form-control" maxLength={500} value={form.externalLink} onChange={set('externalLink')} placeholder="https://leetcode.com/problems/..." />
          </div>
          <div className="col-12">
            <label className="form-label">Description</label>
            <textarea className="form-control" rows={4} required value={form.description} onChange={set('description')} />
          </div>
          <div className="col-12">
            <label className="form-label">{form.type === 'MCQ' ? 'Explanation of the correct answer' : 'Approach notes'}</label>
            <textarea className="form-control" rows={2} value={form.explanation} onChange={set('explanation')} />
          </div>

          {form.type === 'MCQ' && (
            <div className="col-12">
              <label className="form-label">Options (select the correct one)</label>
              {options.map((o, i) => (
                <div className="input-group mb-2" key={i}>
                  <div className="input-group-text">
                    <input className="form-check-input mt-0" type="radio" name="correct" checked={o.correct}
                           onChange={() => markCorrect(i)} aria-label={`Option ${i + 1} is correct`} />
                  </div>
                  <input className="form-control" placeholder={`Option ${i + 1}`} value={o.text}
                         onChange={(e) => setOption(i, { text: e.target.value })} />
                </div>
              ))}
              <button type="button" className="btn btn-sm btn-outline-secondary"
                      disabled={options.length >= 6} onClick={() => setOptions([...options, { text: '', correct: false }])}>
                + Add option
              </button>
            </div>
          )}
        </div>
        <button className="btn btn-primary mt-4">{id ? 'Save changes' : 'Create question'}</button>
      </form>
    </>
  );
}
