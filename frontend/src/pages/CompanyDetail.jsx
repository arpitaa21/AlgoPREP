import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { companyApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function CompanyDetail() {
  const { id } = useParams();
  const [c, setC] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { companyApi.get(id).then(setC).catch((e) => setError(errorMessage(e))); }, [id]);

  if (!c) return error ? <ErrorAlert message={error} /> : <Loading />;
  return (
    <>
      <Link to="/companies" className="small">&larr; All companies</Link>
      <h2 className="mt-2">{c.name}</h2>
      <p className="text-muted">{c.description}</p>
      <ol className="list-group list-group-numbered">
        {c.roadmap.map((s) => (
          <li className="list-group-item" key={s.id}>
            <strong>{s.title}</strong>
            <div className="small">{s.description}</div>
            <div className="small mt-1">
              {s.topicId && <Link className="me-3" to={`/questions?topic=${s.topicId}`}>Practise {s.topicName}</Link>}
              {s.resourceUrl && <a href={s.resourceUrl} target="_blank" rel="noreferrer">Resource &nearr;</a>}
            </div>
          </li>
        ))}
      </ol>
    </>
  );
}
