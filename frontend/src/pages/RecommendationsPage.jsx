import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { me } from '../api/auth.js';
import { getRecommendations } from '../api/recommendations.js';
import DataTable from '../components/DataTable.jsx';
import { Alert, EmptyState, Spinner, StatusBadge } from '../components/Feedback.jsx';
import { errorMessage, fieldErrors } from '../utils/errors.js';
import { formatLabel, formatMoney, todayIso } from '../utils/format.js';

const POLICY_TYPES = ['TERM', 'HEALTH', 'SAVINGS'];

/** FR-2 D: AI-based policy recommendations. */
export default function RecommendationsPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ dateOfBirth: '', insuranceGoals: '', preferredTermYears: '', preferredPolicyType: '' });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);

  // Pre-fill the date of birth from the customer's profile; they can still change it.
  useEffect(() => {
    let active = true;
    me()
      .then((profile) => active && setForm((f) => (f.dateOfBirth ? f : { ...f, dateOfBirth: profile.dateOfBirth })))
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  const update = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  const validate = () => {
    const found = {};
    if (!form.dateOfBirth) found.dateOfBirth = 'Enter your date of birth.';
    else if (form.dateOfBirth >= todayIso()) found.dateOfBirth = 'Date of birth must be in the past.';
    const goals = form.insuranceGoals.trim();
    if (goals.length < 5) found.insuranceGoals = 'Describe your goals in at least 5 characters.';
    else if (goals.length > 500) found.insuranceGoals = 'Keep your goals under 500 characters.';
    if (form.preferredTermYears !== '') {
      const years = Number(form.preferredTermYears);
      if (!Number.isInteger(years) || years < 1 || years > 40) found.preferredTermYears = 'Enter a whole number of years from 1 to 40.';
    }
    setErrors(found);
    return Object.keys(found).length === 0;
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!validate()) return;
    setBusy(true);
    setResult(null);
    try {
      const data = await getRecommendations({
        dateOfBirth: form.dateOfBirth,
        insuranceGoals: form.insuranceGoals.trim(),
        preferredTermYears: form.preferredTermYears === '' ? null : Number(form.preferredTermYears),
        preferredPolicyType: form.preferredPolicyType || null,
      });
      setResult(data);
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const columns = [
    { key: 'productCode', header: 'Product code' },
    { key: 'policyName', header: 'Policy name' },
    { key: 'policyType', header: 'Policy type', render: (row) => formatLabel(row.policyType) },
    { key: 'policyTerm', header: 'Policy term' },
    { key: 'sumInsured', header: 'Sum insured', align: 'right', render: (row) => formatMoney(row.sumInsured) },
    { key: 'premiumFrequency', header: 'Premium frequency', render: (row) => formatLabel(row.premiumFrequency) },
    { key: 'premiumAmount', header: 'Premium amount', align: 'right', render: (row) => formatMoney(row.premiumAmount) },
    { key: 'recommendationReason', header: 'Recommendation reason', render: (row) => <span className="reason">{row.recommendationReason}</span> },
    { key: 'suitability', header: 'Suitability', render: (row) => <StatusBadge value={row.suitability} /> },
    {
      key: 'action',
      header: 'Action',
      render: (row) => (
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={() =>
            navigate(`/register/${encodeURIComponent(row.productCode)}?rec=${row.recommendationItemId}`)
          }
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
          <h1>AI recommendations</h1>
          <p className="muted">Tell us about your goals. We only suggest policies you can register for.</p>
        </div>
      </div>

      <form className="card form form-grid" onSubmit={onSubmit} noValidate>
        <div className="field">
          <label htmlFor="dob">Date of birth</label>
          <input id="dob" type="date" max={todayIso()} value={form.dateOfBirth} onChange={update('dateOfBirth')}
            aria-invalid={Boolean(errors.dateOfBirth)} aria-describedby={errors.dateOfBirth ? 'dob-error' : undefined} />
          {errors.dateOfBirth && <p id="dob-error" className="field-error">{errors.dateOfBirth}</p>}
        </div>
        <div className="field">
          <label htmlFor="term">Preferred policy term (years, optional)</label>
          <input id="term" type="number" min="1" max="40" step="1" value={form.preferredTermYears}
            onChange={update('preferredTermYears')} aria-invalid={Boolean(errors.preferredTermYears)}
            aria-describedby={errors.preferredTermYears ? 'term-error' : undefined} />
          {errors.preferredTermYears && <p id="term-error" className="field-error">{errors.preferredTermYears}</p>}
        </div>
        <div className="field">
          <label htmlFor="type">Insurance type (optional)</label>
          <select id="type" value={form.preferredPolicyType} onChange={update('preferredPolicyType')}>
            <option value="">No preference</option>
            {POLICY_TYPES.map((type) => (
              <option key={type} value={type}>
                {formatLabel(type)}
              </option>
            ))}
          </select>
        </div>
        <div className="field field-wide">
          <label htmlFor="goals">Insurance goals and preferences</label>
          <textarea id="goals" rows="3" maxLength={500} value={form.insuranceGoals} onChange={update('insuranceGoals')}
            placeholder="For example: protect my family's income and cover hospital bills"
            aria-invalid={Boolean(errors.insuranceGoals)} aria-describedby={errors.insuranceGoals ? 'goals-error' : undefined} />
          {errors.insuranceGoals && <p id="goals-error" className="field-error">{errors.insuranceGoals}</p>}
        </div>
        <div className="form-actions field-wide">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Finding policies…' : 'Get recommendations'}
          </button>
        </div>
      </form>

      {busy && <Spinner label="Asking the AI advisor. This can take a few seconds…" />}
      <Alert type="error">{error}</Alert>

      {result && (
        <section className="results" aria-labelledby="results-title">
          <div className="result-meta">
            <h2 id="results-title">Recommended for you</h2>
            <span>Your age: <strong>{result.calculatedAge}</strong></span>
            {result.source !== 'NONE' && <StatusBadge value={result.source} />}
          </div>
          <Alert type={result.source === 'FALLBACK' ? 'info' : 'success'}>{result.message}</Alert>
          {result.items.length === 0 ? (
            <EmptyState title="No recommendations right now." />
          ) : (
            <DataTable caption="Recommended policies" columns={columns} rows={result.items}
              rowKey={(row) => row.recommendationItemId} />
          )}
        </section>
      )}
    </>
  );
}
