import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as authApi from '../api/auth.js';
import { UNAUTHORIZED_EVENT } from '../api/client.js';

const AuthContext = createContext(null);

const LOGGED_OUT = { status: 'out', customer: null };

/**
 * Login state comes from the server's session (Spring Security). On page load the app asks the server
 * whether the browser is logged in, so a refresh keeps the customer logged in until the session ends.
 */
export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', customer: null });
  const navigate = useNavigate();

  const refresh = useCallback(async () => {
    try {
      const data = await authApi.session();
      setState(data.authenticated ? { status: 'in', customer: data.customer } : LOGGED_OUT);
    } catch {
      setState(LOGGED_OUT);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const login = useCallback(async (email, password) => {
    // Make sure the browser has a fresh CSRF cookie before posting the login (e.g. right after a logout).
    await authApi.session();
    const data = await authApi.login(email, password);
    setState({ status: 'in', customer: data.customer });
    return data.customer;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // The session may already be gone; the app logs out either way.
    } finally {
      setState(LOGGED_OUT);
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  // The API client fires this event on any 401: the session ended on the server.
  useEffect(() => {
    const onUnauthorized = () => {
      setState(LOGGED_OUT);
      navigate('/login', { replace: true, state: { expired: true } });
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, [navigate]);

  const value = useMemo(
    () => ({
      status: state.status,
      customer: state.customer,
      isLoggedIn: state.status === 'in',
      login,
      logout,
    }),
    [state, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
