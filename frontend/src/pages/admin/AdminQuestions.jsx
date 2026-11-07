import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../services/endpoints.js';
import { errorMessage } from '../../services/api.js';
import Pagination from '../../components/Pagination.jsx';
import Loading from '../../components/Loading.jsx';
import ErrorAlert from '../../components/ErrorAlert.jsx';
import DifficultyBadge from '../../components/DifficultyBadge.jsx';

export default function AdminQuestions() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const params = { page, size: 15 };
    if (keyword) params.keyword = keyword;
    adminApi.listQuestions(params).then(setData).catch((e) => setError(errorMessage(e)));
  }, [page, keyword]);
  useEffect(() => { load(); }, [load]);

  const remove = async (q) => {
    if (!window.confirm(`Delete "${q.title}"?`)) return;
    setError('');
    try { await adminApi.deleteQuestion(q.id); load(); } catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="mb-0">Manage questions</h2>
        <Link className="btn btn-primary" to="/admin/questions/new">+ New question</Link>
      </div>
      <ErrorAlert message={error} />
      <input className="form-control mb-3" placeholder="Search by title..." value={keyword}
             onChange={(e) => { setKeyword(e.target.value); setPage(0); }} />
      {!data ? <Loading /> : (
        <>
          <table className="table table-hover align-middle">
            <thead><tr><th>ID</th><th>Title</th><th>Topic</th><th>Level</th><th>Type</th><th /></tr></thead>
            <tbody>{data.content.map((q) => (
              <tr key={q.id}>
                <td>{q.id}</td><td>{q.title}</td><td>{q.topicName}</td>
                <td><DifficultyBadge level={q.difficulty} /></td><td>{q.type}</td>
                <td className="text-end text-nowrap">
                  <Link className="btn btn-sm btn-outline-primary me-2" to={`/admin/questions/${q.id}/edit`}>Edit</Link>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => remove(q)}>Delete</button>
                </td>
              </tr>
            ))}</tbody>
          </table>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
