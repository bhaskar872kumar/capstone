import { Navigate, useLocation } from 'react-router-dom';
import { Spinner } from '../components/Feedback.jsx';
import { useAuth } from './AuthContext.jsx';

export default function ProtectedRoute({ children }) {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'loading') {
    return (
      <div className="login-page">
        <Spinner label="Checking your session…" />
      </div>
    );
  }
  if (status !== 'in') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  return children;
}
