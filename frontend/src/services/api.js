import axios from 'axios';

export const TOKEN_KEY = 'pp_token';

// One axios instance for the whole app: base URL and the JWT header are configured once, here.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
});

// Before EVERY request: attach "Authorization: Bearer <token>" if we have a token.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// After EVERY response: if the server says 401 the token is missing/invalid/EXPIRED.
// Pitfall: without this, an expired token makes every page fail silently. We clear it and go to login.
// (A 401 from the login/register call itself just means "wrong credentials", so we leave that alone.)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || '';
    const isAuthCall = url.startsWith('/auth/login') || url.startsWith('/auth/register');
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem(TOKEN_KEY);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?expired=1');
      }
    }
    return Promise.reject(error);
  }
);

/** Turns any axios error into one readable string (the backend always sends {message, fieldErrors}). */
export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors).join(', ');
  }
  return data?.message || err?.message || 'Something went wrong';
}

export default api;
