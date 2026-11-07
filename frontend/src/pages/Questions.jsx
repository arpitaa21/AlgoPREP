import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { bookmarkApi, questionApi, topicApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import QuestionTable from '../components/QuestionTable.jsx';
import Pagination from '../components/Pagination.jsx';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function Questions() {
  const [topics, setTopics] = useState([]);
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState({ topicId: searchParams.get('topic') || '', difficulty: '', type: '', keyword: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    topicApi.list().then(setTopics).catch((e) => setError(errorMessage(e)));
  }, []);

  // Re-runs whenever the filters or the page change. Empty filters are not sent at all.
  const load = useCallback(() => {
    const params = { page, size: 10 };
    Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });
    questionApi.list(params).then(setData).catch((e) => setError(errorMessage(e)));
  }, [filters, page]);

  useEffect(() => { load(); }, [load]);

  const change = (field) => (e) => {
    setFilters({ ...filters, [field]: e.target.value });
    setPage(0);                                  // a new filter always starts from the first page
  };

  const toggleBookmark = async (q) => {
    try {
      q.bookmarked ? await bookmarkApi.remove(q.id) : await bookmarkApi.add(q.id);
      setData({ ...data, content: data.content.map((x) => (x.id === q.id ? { ...x, bookmarked: !x.bookmarked } : x)) });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <>
      <h2 className="mb-3">Question bank</h2>
      <ErrorAlert message={error} />
      <div className="row g-2 mb-3">
        <div className="col-md-3">
          <select className="form-select" value={filters.topicId} onChange={change('topicId')}>
            <option value="">All topics</option>
            {topics.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>
        <div className="col-md-2">
          <select className="form-select" value={filters.difficulty} onChange={change('difficulty')}>
            <option value="">Any difficulty</option>
            <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
          </select>
        </div>
        <div className="col-md-2">
          <select className="form-select" value={filters.type} onChange={change('type')}>
            <option value="">DSA + MCQ</option>
            <option value="DSA">DSA</option><option value="MCQ">MCQ</option>
          </select>
        </div>
        <div className="col-md-5">
          <input className="form-control" placeholder="Search by title..." value={filters.keyword} onChange={change('keyword')} />
        </div>
      </div>

      {!data ? <Loading /> : (
        <>
          <QuestionTable questions={data.content} onToggleBookmark={toggleBookmark} />
          <div className="small text-muted">{data.totalElements} questions</div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
