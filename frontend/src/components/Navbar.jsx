import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const link = ({ isActive }) => `nav-link${isActive ? ' active fw-semibold' : ''}`;

  return (
    <nav className="navbar navbar-expand-lg bg-dark navbar-dark">
      <div className="container">
        <Link className="navbar-brand" to="/">Placement Prep</Link>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav">
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="mainNav">
          {user && (
            <ul className="navbar-nav me-auto">
              <li className="nav-item"><NavLink className={link} to="/" end>Dashboard</NavLink></li>
              <li className="nav-item"><NavLink className={link} to="/questions">Questions</NavLink></li>
              <li className="nav-item"><NavLink className={link} to="/tests">Tests</NavLink></li>
              <li className="nav-item"><NavLink className={link} to="/bookmarks">Bookmarks</NavLink></li>
              <li className="nav-item"><NavLink className={link} to="/revision-lists">Revision</NavLink></li>
              <li className="nav-item"><NavLink className={link} to="/companies">Companies</NavLink></li>
              {isAdmin && <li className="nav-item"><NavLink className={link} to="/admin">Admin</NavLink></li>}
            </ul>
          )}
          <ul className="navbar-nav ms-auto align-items-lg-center">
            {user ? (
              <>
                <li className="nav-item text-light small me-3">{user.name}</li>
                <li className="nav-item">
                  <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>Logout</button>
                </li>
              </>
            ) : (
              <>
                <li className="nav-item"><NavLink className={link} to="/login">Login</NavLink></li>
                <li className="nav-item"><NavLink className={link} to="/register">Register</NavLink></li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
