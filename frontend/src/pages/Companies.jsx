import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { companyApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function Companies() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { companyApi.list().then(setItems).catch((e) => setError(errorMessage(e))); }, []);

  return (
    <>
      <h2 className="mb-3">Company roadmaps</h2>
      <ErrorAlert message={error} />
      {!items ? <Loading /> : (
        <div className="row g-3">
          {items.map((c) => (
            <div className="col-md-4" key={c.id}>
              <div className="card h-100"><div className="card-body">
                <h5>{c.name}</h5>
                <p className="small text-muted">{c.description}</p>
                <Link to={`/companies/${c.id}`} className="btn btn-outline-primary btn-sm">View roadmap ({c.stepCount} steps)</Link>
              </div></div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
