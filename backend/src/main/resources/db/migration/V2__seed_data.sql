-- Sample data from spec section 5.5.1 to 5.5.7 (5.5.8 claim data is future scope and not loaded).
-- Passwords: the spec's placeholders are replaced by real BCrypt hashes of the DEMO_PASSWORD
-- environment variable (Flyway placeholder demo_password). No password is stored in this file.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 5.5.1 customer
INSERT INTO customer (customer_id, email, password_hash, full_name, date_of_birth, phone, status) VALUES
    (1001, 'aarav.sharma@example.com', '{bcrypt}' || crypt('${demo_password}', gen_salt('bf', 10)), 'Aarav Sharma', DATE '1990-04-15', '9876501001', 'ACTIVE'),
    (1002, 'meera.iyer@example.com',   '{bcrypt}' || crypt('${demo_password}', gen_salt('bf', 10)), 'Meera Iyer',   DATE '1985-09-22', '9876501002', 'ACTIVE'),
    (1003, 'rohan.verma@example.com',  '{bcrypt}' || crypt('${demo_password}', gen_salt('bf', 10)), 'Rohan Verma',  DATE '2000-01-10', '9876501003', 'ACTIVE');

-- 5.5.2 insurance_product (age_range 18-60 = min_entry_age 18, max_entry_age 60)
INSERT INTO insurance_product (product_id, product_code, policy_name, policy_type, term_months, premium_frequency,
                               premium_amount, sum_insured, min_entry_age, max_entry_age, active) VALUES
    (201, 'HD-TERM-001',   'HealthDekho Secure Term',   'TERM',    240, 'MONTHLY',   1250.00,  5000000.00, 18, 60, TRUE),
    (202, 'HD-HEALTH-001', 'HealthDekho Family Health', 'HEALTH',  12,  'YEARLY',    18000.00, 1000000.00, 18, 65, TRUE),
    (203, 'HD-SAV-001',    'HealthDekho Smart Savings', 'SAVINGS', 120, 'QUARTERLY', 7500.00,  1500000.00, 21, 55, TRUE);

-- 5.5.3 customer_policy
INSERT INTO customer_policy (policy_id, policy_number, customer_id, product_id, start_date, maturity_date,
                             premium_frequency, premium_amount, sum_insured, total_premium_payable, status) VALUES
    (3001, 'HDP-2026-0001', 1001, 201, DATE '2026-11-20', DATE '2046-11-19', 'MONTHLY', 1250.00,  5000000.00, 300000.00, 'ACTIVE'),
    (3002, 'HDP-2026-0002', 1002, 202, DATE '2026-11-20', DATE '2027-11-19', 'YEARLY',  18000.00, 1000000.00, 18000.00,  'ACTIVE');

-- 5.5.4 premium_installment (sample rows)
INSERT INTO premium_installment (installment_id, policy_id, installment_number, due_date, grace_period_end_date,
                                 scheduled_amount, paid_amount, payment_status) VALUES
    (4001, 3001, 1, DATE '2026-11-20', DATE '2026-12-20', 1250.00,  1250.00,  'PAID'),
    (4002, 3001, 2, DATE '2026-12-20', DATE '2027-01-20', 1250.00,  500.00,   'PARTIALLY_PAID'),
    (4003, 3001, 3, DATE '2027-01-20', DATE '2027-02-20', 1250.00,  0.00,     'PENDING'),
    (4004, 3002, 1, DATE '2026-11-20', DATE '2026-12-20', 18000.00, 18000.00, 'PAID');

SELECT setval(pg_get_serial_sequence('premium_installment', 'installment_id'), (SELECT MAX(installment_id) FROM premium_installment));

-- Policy 3001 has total premium 300000.00 = 240 monthly installments of 1250.00, but 5.5.4 lists only 1 to 3.
-- Generate installments 4 to 240 with the same rule the application uses:
-- due date = start + (n - 1) months, grace period end = due date + 1 month.
INSERT INTO premium_installment (policy_id, installment_number, due_date, grace_period_end_date,
                                 scheduled_amount, paid_amount, payment_status)
SELECT 3001,
       n,
       (DATE '2026-11-20' + make_interval(months => n - 1))::date,
       ((DATE '2026-11-20' + make_interval(months => n - 1)) + INTERVAL '1 month')::date,
       1250.00,
       0.00,
       'PENDING'
FROM generate_series(4, 240) AS n;

-- 5.5.5 premium_payment
INSERT INTO premium_payment (payment_id, installment_id, transaction_reference, payment_date, amount_paid, penalty_amount, payment_method) VALUES
    (5001, 4001, 'TXN-HD-000001', DATE '2026-11-20', 1250.00,  0.00, 'UPI'),
    (5002, 4002, 'TXN-HD-000002', DATE '2026-12-20', 500.00,   0.00, 'NET_BANKING'),
    (5003, 4004, 'TXN-HD-000003', DATE '2026-11-20', 18000.00, 0.00, 'CARD');

-- 5.5.6 ai_recommendation_request
INSERT INTO ai_recommendation_request (recommendation_request_id, customer_id, insurance_goals, preferred_term,
                                       preferred_policy_type, calculated_age, model_name, model_version, prompt_version, requested_at) VALUES
    (6001, 1001, 'Family income protection',             '20 years', 'TERM',   36, 'configured-chat-model', 'deployment-v1', 'rec-v1', TIMESTAMPTZ '2026-11-20 10:15:00+05:30'),
    (6002, 1003, 'Health coverage for self and parents', '1 year',   'HEALTH', 26, 'configured-chat-model', 'deployment-v1', 'rec-v1', TIMESTAMPTZ '2026-11-20 10:30:00+05:30');

-- 5.5.7 ai_recommendation_item
INSERT INTO ai_recommendation_item (recommendation_item_id, recommendation_request_id, product_id, rank_order,
                                    recommendation_reason, suitability, selected_for_registration) VALUES
    (7001, 6001, 201, 1, 'Long-term protection aligned with income-replacement goal', 'HIGH',   TRUE),
    (7002, 6001, 203, 2, 'Savings option supports a secondary wealth goal',           'MEDIUM', FALSE),
    (7003, 6002, 202, 1, 'Annual family health cover matches stated preference',      'HIGH',   FALSE);

-- Move every identity past the explicit sample ids so new rows don't collide.
SELECT setval(pg_get_serial_sequence('customer', 'customer_id'), (SELECT MAX(customer_id) FROM customer));
SELECT setval(pg_get_serial_sequence('insurance_product', 'product_id'), (SELECT MAX(product_id) FROM insurance_product));
SELECT setval(pg_get_serial_sequence('customer_policy', 'policy_id'), (SELECT MAX(policy_id) FROM customer_policy));
SELECT setval(pg_get_serial_sequence('premium_payment', 'payment_id'), (SELECT MAX(payment_id) FROM premium_payment));
SELECT setval(pg_get_serial_sequence('ai_recommendation_request', 'recommendation_request_id'), (SELECT MAX(recommendation_request_id) FROM ai_recommendation_request));
SELECT setval(pg_get_serial_sequence('ai_recommendation_item', 'recommendation_item_id'), (SELECT MAX(recommendation_item_id) FROM ai_recommendation_item));
