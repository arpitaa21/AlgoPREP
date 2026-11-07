import { createContext, useContext, useEffect, useState } from 'react';
import { TOKEN_KEY } from '../services/api.js';
import { authApi } from '../services/endpoints.js';

// React Context = a global "who is logged in" value any component can read without prop-drilling.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);   // true while we check a stored token on page load

  // On first load: if a token is stored, ask the server who it belongs to.
  // If the token is expired the server answers 401 and we simply start logged out.
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false));
  }, []);

  const handleAuthResponse = (response) => {
    localStorage.setItem(TOKEN_KEY, response.token);
    setUser(response.user);
    return response.user;
  };

  const login = async (email, password) => handleAuthResponse(await authApi.login({ email, password }));
  const register = async (name, email, password) =>
    handleAuthResponse(await authApi.register({ name, email, password }));
  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  };

  const isAdmin = !!user && user.roles.includes('ADMIN');

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
