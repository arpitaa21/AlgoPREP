import { useEffect, useState } from 'react';
import { adminApi, companyApi, topicApi } from '../../services/endpoints.js';
import { errorMessage } from '../../services/api.js';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import Loading from '../../components/Loading.jsx';

export default function AdminCompanies() {
  const [companies, setCompanies] = useState(null);
  const [topics, setTopics] = useState([]);
  const [open, setOpen] = useState(null);                 // company detail with roadmap
  const [form, setForm] = useState({ name: '', description: '' });
  const [step, setStep] = useState({ stepOrder: 1, title: '', description: '', topicId: '', resourceUrl: '' });
  const [error, setError] = useState('');

  const load = () => companyApi.list().then(setCompanies).catch((e) => setError(errorMessage(e)));
  useEffect(() => { load(); topicApi.list().then(setTopics).catch(() => {}); }, []);

  const guard = async (fn) => { setError(''); try { await fn(); } catch (e) { setError(errorMessage(e)); } };
  const openCompany = (id) => guard(async () => setOpen(await companyApi.get(id)));

  const addCompany = (e) => { e.preventDefault(); guard(async () => {
    await adminApi.createCompany(form); setForm({ name: '', description: '' }); await load();
  }); };
  const removeCompany = (c) => window.confirm(`Delete ${c.name} and its roadmap?`) && guard(async () => {
    await adminApi.deleteCompany(c.id); if (open?.id === c.id) setOpen(null); await load();
  });
  const addStep = (e) => { e.preventDefault(); guard(async () => {
    await adminApi.addStep(open.id, { ...step, stepOrder: Number(step.stepOrder), topicId: step.topicId ? Number(step.topicId) : null,
      resourceUrl: step.resourceUrl || null });
    setStep({ stepOrder: step.stepOrder + 1, title: '', description: '', topicId: '', resourceUrl: '' });
    await openCompany(open.id); await load();
  }); };
  const removeStep = (s) => guard(async () => { await adminApi.deleteStep(s.id); await openCompany(open.id); await load(); });

  return (
    <>
      <h2 className="mb-3">Manage companies</h2>
      <ErrorAlert message={error} />
      <form className="row g-2 mb-4" onSubmit={addCompany}>
        <div className="col-md-3"><input className="form-control" placeholder="Company name" required value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
        <div className="col-md-7"><input className="form-control" placeholder="Short description" value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
        <div className="col-md-2"><button className="btn btn-primary w-100">Add company</button></div>
      </form>

      <div className="row g-4">
        <div className="col-md-4">
          {!companies ? <Loading /> : (
            <ul className="list-group">
              {companies.map((c) => (
                <li key={c.id} className={`list-group-item d-flex justify-content-between align-items-center ${open?.id === c.id ? 'active' : ''}`}>
                  <button className="btn btn-link p-0 text-reset text-decoration-none" onClick={() => openCompany(c.id)}>{c.name} ({c.stepCount})</button>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => removeCompany(c)}>x</button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="col-md-8">
          {open && (
            <>
              <h5>{open.name} roadmap</h5>
              <ol className="list-group list-group-numbered mb-3">
                {open.roadmap.map((s) => (
                  <li key={s.id} className="list-group-item d-flex justify-content-between">
                    <span><strong>{s.title}</strong> <span className="text-muted small">{s.topicName}</span></span>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => removeStep(s)}>Remove</button>
                  </li>
                ))}
              </ol>
              <form className="card card-body" onSubmit={addStep}>
                <div className="row g-2">
                  <div className="col-3"><input className="form-control" type="number" min="1" required value={step.stepOrder} title="Order"
                    onChange={(e) => setStep({ ...step, stepOrder: e.target.value })} /></div>
                  <div className="col-9"><input className="form-control" placeholder="Step title" required value={step.title}
                    onChange={(e) => setStep({ ...step, title: e.target.value })} /></div>
                  <div className="col-12"><input className="form-control" placeholder="Description" value={step.description}
                    onChange={(e) => setStep({ ...step, description: e.target.value })} /></div>
                  <div className="col-6">
                    <select className="form-select" value={step.topicId} onChange={(e) => setStep({ ...step, topicId: e.target.value })}>
                      <option value="">No linked topic</option>
                      {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
                    </select>
                  </div>
                  <div className="col-6"><input className="form-control" placeholder="Resource URL (optional)" value={step.resourceUrl}
                    onChange={(e) => setStep({ ...step, resourceUrl: e.target.value })} /></div>
                </div>
                <button className="btn btn-primary btn-sm mt-2 align-self-start">Add step</button>
              </form>
            </>
          )}
        </div>
      </div>
    </>
  );
}
