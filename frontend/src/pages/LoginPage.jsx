import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { Alert, Spinner } from '../components/Feedback.jsx';
import { errorMessage } from '../utils/errors.js';
import { EMAIL_PATTERN } from '../utils/validation.js';

/** FR-1 A: customer login. */
export default function LoginPage() {
  const { login, isLoggedIn, status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isLoggedIn) {
    return <Navigate to="/dashboard" replace />;
  }
  if (status === 'loading') {
    return (
      <div className="login-page">
        <Spinner label="Checking your session…" />
      </div>
    );
  }

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  const validate = () => {
    const found = {};
    if (!form.email.trim()) found.email = 'Enter your email.';
    else if (!EMAIL_PATTERN.test(form.email.trim())) found.email = 'Enter a valid email address.';
    if (!form.password) found.password = 'Enter your password.';
    setErrors(found);
    return Object.keys(found).length === 0;
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!validate()) return;
    setBusy(true);
    try {
      await login(form.email.trim(), form.password);
      navigate(location.state?.from || '/dashboard', { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Invalid email or password'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <p className="brand brand-lg">
          <span className="brand-mark" aria-hidden="true">+</span>
          HealthDekho
        </p>
        <h1 id="login-title">Log in to your account</h1>
        <p className="muted">See your policies, get recommendations and pay premiums.</p>

        {location.state?.expired && <Alert type="info">Your session ended. Please log in again.</Alert>}
        <Alert type="error">{error}</Alert>

        <form onSubmit={onSubmit} noValidate className="form">
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              value={form.email}
              onChange={update('email')}
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'email-error' : undefined}
            />
            {errors.email && <p id="email-error" className="field-error">{errors.email}</p>}
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={update('password')}
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'password-error' : undefined}
            />
            {errors.password && <p id="password-error" className="field-error">{errors.password}</p>}
          </div>
          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Logging in…' : 'Log in'}
          </button>
        </form>
      </section>
    </div>
  );
}
