import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../services/endpoints.js';
import { errorMessage } from '../../services/api.js';
import Loading from '../../components/Loading.jsx';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import { formatDateTime } from '../../utils/format.js';

export default function AdminDashboard() {
  const [d, setD] = useState(null);
  const [users, setUsers] = useState([]);
  const [topicName, setTopicName] = useState('');
  const [error, setError] = useState('');

  const load = () => {
    adminApi.dashboard().then(setD).catch((e) => setError(errorMessage(e)));
    adminApi.users().then(setUsers).catch(() => {});
  };
  useEffect(load, []);

  const addTopic = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await adminApi.createTopic({ name: topicName.trim() });
      setTopicName('');
      load();
    } catch (err) { setError(errorMessage(err)); }
  };

  if (!d) return error ? <ErrorAlert message={error} /> : <Loading />;

  const tiles = [
    ['Users', d.totalUsers], ['Students', d.totalStudents], ['Questions', d.totalQuestions],
    ['Tests', d.totalTests], ['Test attempts', d.totalAttempts], ['Submissions', d.totalSubmissions],
    ['Avg test score', `${d.averageTestPercentage}%`],
  ];

  return (
    <>
      <h2 className="mb-3">Admin dashboard</h2>
      <ErrorAlert message={error} />
      <div className="d-flex gap-2 mb-3 flex-wrap">
        <Link className="btn btn-primary" to="/admin/questions/new">+ New question</Link>
        <Link className="btn btn-outline-primary" to="/admin/questions">Manage questions</Link>
        <Link className="btn btn-outline-primary" to="/admin/tests">Manage tests</Link>
        <Link className="btn btn-outline-primary" to="/admin/companies">Manage companies</Link>
      </div>

      <div className="row g-3 mb-4">
        {tiles.map(([label, value]) => (
          <div className="col-6 col-md-3" key={label}><div className="card text-center"><div className="card-body py-3">
            <div className="fs-3 fw-bold">{value}</div><div className="small text-muted">{label}</div>
          </div></div></div>
        ))}
      </div>

      <div className="row g-4">
        <div className="col-md-6">
          <h5>Questions per topic</h5>
          <table className="table table-sm"><tbody>
            {d.questionsByTopic.map((t) => <tr key={t.topic}><td>{t.topic}</td><td className="text-end">{t.total}</td></tr>)}
          </tbody></table>
          <form className="d-flex gap-2" onSubmit={addTopic}>
            <input className="form-control form-control-sm" placeholder="New topic name" value={topicName}
                   onChange={(e) => setTopicName(e.target.value)} required />
            <button className="btn btn-sm btn-outline-primary text-nowrap">Add topic</button>
          </form>
        </div>
        <div className="col-md-6">
          <h5>By difficulty / type</h5>
          <p className="mb-1">
            {Object.entries(d.questionsByDifficulty).map(([k, v]) => `${k}: ${v}`).join('  |  ')}
          </p>
          <p>{Object.entries(d.questionsByType).map(([k, v]) => `${k}: ${v}`).join('  |  ')}</p>
          <h5>Users</h5>
          <div style={{ maxHeight: 220, overflow: 'auto' }}>
            <table className="table table-sm"><tbody>
              {users.map((u) => (
                <tr key={u.id}><td>{u.name}<div className="small text-muted">{u.email}</div></td>
                  <td>{u.roles.join(', ')}</td><td className="small">{formatDateTime(u.createdAt)}</td></tr>
              ))}
            </tbody></table>
          </div>
        </div>
      </div>
    </>
  );
}
