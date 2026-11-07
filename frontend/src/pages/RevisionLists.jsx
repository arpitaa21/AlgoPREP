import { useEffect, useState } from 'react';
import { revisionApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import QuestionTable from '../components/QuestionTable.jsx';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function RevisionLists() {
  const [lists, setLists] = useState(null);
  const [open, setOpen] = useState(null);       // the list whose questions are displayed
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const refresh = () => revisionApi.list().then(setLists).catch((e) => setError(errorMessage(e)));
  useEffect(() => { refresh(); }, []);

  const guard = async (fn) => {
    setError('');
    try { await fn(); } catch (e) { setError(errorMessage(e)); }
  };

  const create = (e) => {
    e.preventDefault();
    guard(async () => { await revisionApi.create(name.trim()); setName(''); await refresh(); });
  };
  const show = (id) => guard(async () => setOpen(await revisionApi.get(id)));
  const removeList = (id) => guard(async () => {
    await revisionApi.remove(id);
    if (open?.id === id) setOpen(null);
    await refresh();
  });
  const removeQuestion = (q) => guard(async () => {
    setOpen(await revisionApi.removeQuestion(open.id, q.id));
    await refresh();
  });

  return (
    <>
      <h2 className="mb-3">Revision lists</h2>
      <ErrorAlert message={error} />
      <form className="d-flex gap-2 mb-3" onSubmit={create}>
        <input className="form-control" placeholder="New list name, e.g. Before Amazon OA" value={name} maxLength={100}
               onChange={(e) => setName(e.target.value)} required />
        <button className="btn btn-primary text-nowrap">Create list</button>
      </form>

      {!lists ? <Loading /> : lists.length === 0 ? <p className="text-muted">No lists yet. Create one, then add questions from a question page.</p> : (
        <ul className="list-group mb-4">
          {lists.map((l) => (
            <li key={l.id} className={`list-group-item d-flex justify-content-between align-items-center ${open?.id === l.id ? 'active' : ''}`}>
              <button className="btn btn-link p-0 text-start text-reset text-decoration-none" onClick={() => show(l.id)}>
                {l.name} <span className="badge text-bg-secondary ms-1">{l.questionCount}</span>
              </button>
              <button className="btn btn-sm btn-outline-danger" onClick={() => removeList(l.id)}>Delete</button>
            </li>
          ))}
        </ul>
      )}

      {open && (
        <>
          <h5>{open.name}</h5>
          <QuestionTable questions={open.questions} onRemove={removeQuestion} />
        </>
      )}
    </>
  );
}
