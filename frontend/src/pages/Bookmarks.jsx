import { useEffect, useState } from 'react';
import { bookmarkApi } from '../services/endpoints.js';
import { errorMessage } from '../services/api.js';
import QuestionTable from '../components/QuestionTable.jsx';
import Loading from '../components/Loading.jsx';
import ErrorAlert from '../components/ErrorAlert.jsx';

export default function Bookmarks() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { bookmarkApi.list().then(setItems).catch((e) => setError(errorMessage(e))); }, []);

  const remove = async (q) => {
    try {
      await bookmarkApi.remove(q.id);
      setItems(items.filter((x) => x.id !== q.id));
    } catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <h2 className="mb-3">Bookmarks</h2>
      <ErrorAlert message={error} />
      {!items ? <Loading /> : <QuestionTable questions={items} onRemove={remove} removeLabel="Unbookmark" />}
    </>
  );
}
