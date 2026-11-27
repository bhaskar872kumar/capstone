import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { availableProducts } from '../api/products.js';
import DataTable from '../components/DataTable.jsx';
import { Alert, EmptyState, Spinner } from '../components/Feedback.jsx';
import { errorMessage } from '../utils/errors.js';
import { formatLabel, formatMoney } from '../utils/format.js';

/** FR-1 C: active products the customer hasn't registered yet. */
export default function ProductsPage() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    availableProducts()
      .then((data) => active && setProducts(data))
      .catch((err) => active && setError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  const types = useMemo(() => [...new Set(products.map((p) => p.policyType))].sort(), [products]);
  const visible = typeFilter === 'ALL' ? products : products.filter((p) => p.policyType === typeFilter);

  const columns = [
    { key: 'productCode', header: 'Product code' },
    { key: 'policyName', header: 'Policy name' },
    { key: 'policyType', header: 'Policy type', render: (row) => formatLabel(row.policyType) },
    { key: 'policyTerm', header: 'Policy term' },
    { key: 'sumInsured', header: 'Sum insured', align: 'right', render: (row) => formatMoney(row.sumInsured) },
    { key: 'premiumFrequency', header: 'Premium frequency', render: (row) => formatLabel(row.premiumFrequency) },
    { key: 'premiumAmount', header: 'Premium amount', align: 'right', render: (row) => formatMoney(row.premiumAmount) },
    { key: 'age', header: 'Entry age', render: (row) => `${row.minEntryAge}–${row.maxEntryAge}` },
    {
      key: 'action',
      header: 'Action',
      render: (row) => (
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={() => navigate(`/register/${encodeURIComponent(row.productCode)}`)}
          aria-label={`Register for ${row.policyName}`}
        >
          Register
        </button>
      ),
    },
  ];

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Available policies</h1>
          <p className="muted">Policies you can still register for.</p>
        </div>
        {types.length > 1 && (
          <div className="field field-inline">
            <label htmlFor="type-filter">Policy type</label>
            <select id="type-filter" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
              <option value="ALL">All types</option>
              {types.map((type) => (
                <option key={type} value={type}>
                  {formatLabel(type)}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {loading && <Spinner label="Loading policies…" />}
      <Alert type="error">{error}</Alert>

      {!loading && !error && visible.length === 0 && (
        <EmptyState title="No policies to show.">
          <p>You have already registered for every policy we offer{typeFilter !== 'ALL' ? ' of this type' : ''}.</p>
        </EmptyState>
      )}

      {!loading && visible.length > 0 && (
        <DataTable caption="Available policies" columns={columns} rows={visible} rowKey={(row) => row.productCode} />
      )}
    </>
  );
}
