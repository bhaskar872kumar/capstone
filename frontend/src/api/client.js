import axios from 'axios';

export const UNAUTHORIZED_EVENT = 'healthdekho:unauthorized';

// One axios instance for the whole app (spec 2.2: REST API integration using Axios).
// Login is kept in the server's session cookie. Spring Security's CSRF token comes in the XSRF-TOKEN cookie,
// and axios sends it back automatically in the X-XSRF-TOKEN header on same-origin requests.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  },
);

export default api;
