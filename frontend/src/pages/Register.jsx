import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { errorMessage } from '../services/api.js';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await register(form.name, form.email, form.password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <h2 className="mb-3">Create account</h2>
        <ErrorAlert message={error} />
        <form onSubmit={submit}>
          <div className="mb-3">
            <label className="form-label">Name</label>
            <input className="form-control" required value={form.name} onChange={set('name')} />
          </div>
          <div className="mb-3">
            <label className="form-label">Email</label>
            <input className="form-control" type="email" required value={form.email} onChange={set('email')} />
          </div>
          <div className="mb-3">
            <label className="form-label">Password (8-72 characters)</label>
            <input className="form-control" type="password" minLength={8} maxLength={72} required value={form.password} onChange={set('password')} />
          </div>
          <button className="btn btn-primary w-100" disabled={busy}>{busy ? 'Creating...' : 'Register'}</button>
        </form>
        <p className="mt-3 small">Already registered? <Link to="/login">Login</Link></p>
      </div>
    </div>
  );
}
