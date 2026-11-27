import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { getQuote, registerPolicy } from '../api/policies.js';
import { Alert, Spinner } from '../components/Feedback.jsx';
import { errorMessage } from '../utils/errors.js';
import { formatDate, formatLabel, formatMoney } from '../utils/format.js';

/** FR-3: policy registration with every value pre-populated and calculated by the server. */
export default function RegisterPage() {
  const { productCode } = useParams();
  const [searchParams] = useSearchParams();
  const recommendationItemId = searchParams.get('rec');

  const [quote, setQuote] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [registered, setRegistered] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getQuote(productCode)
      .then((data) => active && setQuote(data))
      .catch((err) => active && setLoadError(errorMessage(err)))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [productCode]);

  const onConfirm = async () => {
    setSubmitError('');
    setSubmitting(true);
    try {
      setRegistered(await registerPolicy(productCode, recommendationItemId));
    } catch (err) {
      setSubmitError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Spinner label="Preparing your registration…" />;
  if (loadError) {
    return (
      <>
        <h1>Register a policy</h1>
        <Alert type="error">{loadError}</Alert>
        <Link to="/products">Back to available policies</Link>
      </>
    );
  }

  if (registered) {
    return (
      <section className="card success-card" aria-labelledby="done-title">
        <h1 id="done-title">Policy registered</h1>
        <Alert type="success" title={`Policy ID: ${registered.policyNumber}`}>
          {registered.message}
        </Alert>
        <dl className="details">
          <Detail label="Start date" value={formatDate(registered.policyStartDate)} />
          <Detail label="Maturity date" value={formatDate(registered.policyMaturityDate)} />
          <Detail label="Total premium payable" value={formatMoney(registered.totalPremiumPayable)} />
          <Detail label="Installments" value={registered.installmentCount} />
          <Detail label="First premium due" value={formatDate(registered.firstDueDate)} />
        </dl>
        <div className="form-actions">
          <Link to="/dashboard" className="btn btn-primary">Go to dashboard</Link>
          <Link to="/products" className="btn btn-ghost">See more policies</Link>
        </div>
      </section>
    );
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Register a policy</h1>
          <p className="muted">Check the details below, then confirm.</p>
        </div>
      </div>

      <section className="card" aria-labelledby="customer-title">
        <h2 id="customer-title">Your details</h2>
        <dl className="details">
          <Detail label="Customer name" value={quote.customerName} />
          <Detail label="Date of birth" value={formatDate(quote.dateOfBirth)} />
          <Detail label="Contact information" value={`${quote.email} · ${quote.phone}`} />
        </dl>
      </section>

      <section className="card" aria-labelledby="policy-title">
        <h2 id="policy-title">Policy details</h2>
        <dl className="details">
          <Detail label="Policy type" value={formatLabel(quote.policyType)} />
          <Detail label="Policy name" value={quote.policyName} />
          <Detail label="Policy term" value={quote.policyTerm} />
          <Detail label="Policy start date" value={formatDate(quote.policyStartDate)} />
          <Detail label="Policy maturity date" value={formatDate(quote.policyMaturityDate)} />
          <Detail label="Premium payment frequency" value={formatLabel(quote.premiumFrequency)} />
          <Detail label="Premium amount" value={formatMoney(quote.premiumAmount)} />
          <Detail label="Sum insured" value={formatMoney(quote.sumInsured)} />
          <Detail label="Total premium payable" value={`${formatMoney(quote.totalPremiumPayable)} (${quote.installmentCount} installments)`} strong />
        </dl>
      </section>

      {quote.eligible ? (
        <Alert type="success">
          You're eligible: this policy is for ages {quote.minEntryAge} to {quote.maxEntryAge} and you'll be{' '}
          {quote.ageOnStartDate} on the start date.
        </Alert>
      ) : (
        <Alert type="error" title="You can't register for this policy">{quote.ineligibleReason}</Alert>
      )}
      <Alert type="error">{submitError}</Alert>

      <div className="form-actions">
        <button type="button" className="btn btn-primary" onClick={onConfirm} disabled={!quote.eligible || submitting}>
          {submitting ? 'Registering…' : 'Confirm registration'}
        </button>
        <Link to="/products" className="btn btn-ghost">Cancel</Link>
      </div>
    </>
  );
}

function Detail({ label, value, strong = false }) {
  return (
    <div className="detail">
      <dt>{label}</dt>
      <dd className={strong ? 'strong' : undefined}>{value ?? '—'}</dd>
    </div>
  );
}
