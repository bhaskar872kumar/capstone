import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { myPolicies } from '../api/policies.js';
import { useAuth } from '../auth/AuthContext.jsx';
import DataTable from '../components/DataTable.jsx';
import { Alert, EmptyState, Spinner, StatusBadge } from '../components/Feedback.jsx';
import { errorMessage } from '../utils/errors.js';
import { formatDate, formatLabel, formatMoney } from '../utils/format.js';

const COLUMNS = [
  { key: 'policyNumber', header: 'Policy ID' },
  { key: 'policyName', header: 'Policy name' },
  { key: 'policyType', header: 'Policy type', render: (row) => formatLabel(row.policyType) },
  { key: 'policyTerm', header: 'Policy term' },
  { key: 'policyStartDate', header: 'Start date', render: (row) => formatDate(row.policyStartDate) },
  { key: 'policyMaturityDate', header: 'Maturity date', render: (row) => formatDate(row.policyMaturityDate) },
  { key: 'premiumFrequency', header: 'Premium frequency', render: (row) => formatLabel(row.premiumFrequency) },
  { key: 'premiumAmount', header: 'Premium amount', align: 'right', render: (row) => formatMoney(row.premiumAmount) },
  { key: 'sumInsured', header: 'Sum insured', align: 'right', render: (row) => formatMoney(row.sumInsured) },
  { key: 'policyStatus', header: 'Status', render: (row) => <StatusBadge value={row.policyStatus} /> },
];

/** FR-1 B: the logged-in customer's registered policies. */
export default function DashboardPage() {
  const { customer } = useAuth();
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    myPolicies()
      .then((data) => active && setPolicies(data))
      .catch((err) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Welcome, {customer?.fullName?.split(' ')[0]}</h1>
          <p className="muted">Your registered insurance policies.</p>
        </div>
        <Link to="/recommendations" className="btn btn-primary">
          Get AI recommendations
        </Link>
      </div>

      {loading && <Spinner label="Loading your policies…" />}
      <Alert type="error">{error}</Alert>

      {!loading && !error && policies.length === 0 && (
        <EmptyState title="You have not registered any insurance policy yet.">
          <p>
            Browse the <Link to="/products">available policies</Link> or{' '}
            <Link to="/recommendations">get AI recommendations</Link>.
          </p>
        </EmptyState>
      )}

      {!loading && policies.length > 0 && (
        <DataTable caption="My policies" columns={COLUMNS} rows={policies} rowKey={(row) => row.policyNumber} />
      )}
    </>
  );
}
