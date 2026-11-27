# Schema migrations (Flyway)

Location: `backend/src/main/resources/db/migration`. Flyway runs them on every start, in order, once each.
History is kept in the `flyway_schema_history` table. Applied files are never edited; changes go in a new file.

| File | What it does |
|---|---|
| `V1__schema.sql` | Creates the 7 tables of spec 5.2 with primary keys, foreign keys, unique keys, checks and indexes, plus the `policy_number_seq` sequence for new policy IDs. No claim tables. |
| `V2__seed_data.sql` | Loads the sample data of spec 5.5.1–5.5.7. Passwords are BCrypt hashes of `DEMO_PASSWORD`. Generates installments 4–240 of policy HDP-2026-0001 so its schedule matches its 300000.00 total. |

Notes:
- `premium_installment` is written by FR-3 registration (the installment schedule).
- `premium_payment` is kept because it is part of the spec's database design (5.2) and sample data (5.5.5);
  no application code writes to it since FR-4 (payments) was removed from scope.
- The customer table doubles as Spring Security's user store: login reads `email`, `password_hash` and
  `status = 'ACTIVE'` through `JdbcUserDetailsManager`.
