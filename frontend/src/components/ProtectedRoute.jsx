import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import Loading from './Loading.jsx';

/**
 * Wraps pages that need login. adminOnly pages additionally need the ADMIN role.
 * IMPORTANT: this only improves the user experience. Real security is enforced by the backend (401/403);
 * anyone can edit JavaScript in their browser, so the UI can never be trusted to protect data.
 */
export default function ProtectedRoute({ adminOnly = false, children }) {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();

  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />;
  return children;
}
