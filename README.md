# HealthDekho – Insurance Management System

Full-stack capstone: customers log in, see their policies, browse available policies, get AI recommendations
and register policies.

| | |
|---|---|
| Backend | Java 21, Spring Boot 3.5.7, Spring Web, Spring Security (JDBC authentication), Spring Data JPA, PostgreSQL, Flyway, Spring AI (Amazon Bedrock) |
| Frontend | React 18 (Vite), React Router, Axios |
| Deploy | One JAR (React inside) on AWS Elastic Beanstalk + Amazon RDS PostgreSQL |

## Scope

| Story | Feature | Main code |
|---|---|---|
| US-01 | Login (FR-1 A) | `security/` |
| US-02 | Dashboard (FR-1 B) | `policy/PolicyService` |
| US-03 | Available policies (FR-1 C) | `product/ProductService` |
| US-04 | AI recommendations (FR-2) | `recommendation/` |
| US-05 | Policy registration (FR-3) | `policy/RegistrationService`, `PolicyCalculator` |
| – | Premium payments (FR-4) | Removed from scope by the spec update |
| – | Policy search (FR-5) | Removed from scope by the spec update |
| US-08 | AI claim assessment (FE-1) | Future scope – not built |

## How login works

Spring Security's standard JDBC authentication (`JdbcUserDetailsManager`) reads customers straight from the
`customer` table: `email` is the username, `password_hash` the BCrypt password, and only `ACTIVE` customers can log in.

1. `POST /api/auth/login` checks the email and password and stores the logged-in customer in the HTTP session
   (`JSESSIONID` cookie, HttpOnly, 30 minutes).
2. Every other `/api/**` call needs that session. `POST /api/auth/logout` ends it.
3. Changes (POST) are protected against CSRF: the server sets an `XSRF-TOKEN` cookie and the React app sends it back
   in the `X-XSRF-TOKEN` header (axios does this automatically).

## Project layout

```
healthdekho/
├── backend/                Spring Boot API
│   ├── src/main/java/com/healthdekho/
│   │   ├── security/       JDBC authentication, session login/logout, current customer
│   │   ├── customer/       customer profile
│   │   ├── product/        product catalog, available products
│   │   ├── policy/         dashboard, registration, calculator, installment schedule
│   │   ├── recommendation/ Spring AI client, fallback ranking, saved recommendations
│   │   ├── common/         errors (ProblemDetail), money helpers
│   │   └── config/         clock, React route forwarding
│   └── src/main/resources/
│       ├── db/migration/   V1__schema.sql (7 tables), V2__seed_data.sql (spec sample data)
│       └── prompts/        recommendation-v1.txt
├── frontend/               React app
├── scripts/                local settings templates, release build
└── docs/                   runbook, test catalog, schema migrations
```

## Run it locally

You need: JDK 21, Maven 3.9+, Node.js 20+, PostgreSQL 16, and AWS credentials with Bedrock access (optional:
without them recommendations use the rule-based fallback).

**1. Databases** (once). In psql or pgAdmin as the `postgres` user:

```sql
CREATE ROLE healthdekho LOGIN PASSWORD 'choose-a-password';
CREATE DATABASE healthdekho OWNER healthdekho;
CREATE DATABASE healthdekho_test OWNER healthdekho;
```

**2. Settings** (once). Copy the template and fill it in. Set `DEMO_PASSWORD` before the first start:
the seed migration hashes it for the three sample customers.

```bash
cp scripts/local-env.example.sh scripts/local-env.sh      # Windows: Copy-Item scripts\local-env.example.ps1 scripts\local-env.ps1
```

**3. Backend** (port 8080). Flyway creates the tables and loads the sample data on first start.

```bash
source scripts/local-env.sh                               # Windows: . .\scripts\local-env.ps1
cd backend
mvn spring-boot:run
```

**4. Frontend** (port 5173, calls the backend through the Vite proxy, so the session and CSRF cookies work).
In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in as one of the sample customers with your `DEMO_PASSWORD`:

| Customer | Email | What you'll see |
|---|---|---|
| Aarav Sharma | aarav.sharma@example.com | 1 policy (HDP-2026-0001) |
| Meera Iyer | meera.iyer@example.com | 1 policy (HDP-2026-0002) |
| Rohan Verma | rohan.verma@example.com | No policies (empty dashboard) |

No Maven installed? Add the Maven wrapper once with `mvn -N wrapper:wrapper` on a machine that has Maven,
or open `backend/` in VS Code / IntelliJ and use its built-in Maven.

## Tests and coverage

Tests run against the separate `healthdekho_test` database (the sample data is loaded there too).

```bash
source scripts/local-env.sh
cd backend
mvn verify
```

- Results: console summary, `backend/target/surefire-reports/`
- Coverage report: `backend/target/site/jacoco/index.html`
- Test list with ids: [docs/test-catalog.md](docs/test-catalog.md)

## Release build and AWS

```bash
./scripts/build-release.sh          # Windows: .\scripts\build-release.ps1
```

This builds `backend/target/healthdekho.jar` with the React app inside. Run it with
`java -jar backend/target/healthdekho.jar` and open http://localhost:8080.
Deployment to AWS: [docs/runbook.md](docs/runbook.md).

## Configuration

All secrets come from environment variables, never from files in the repo.

| Variable | Used for | Secret |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` or `aws` | No |
| `DB_URL`, `DB_USERNAME` | Database connection | No |
| `DB_PASSWORD` | Database password | Yes |
| `DEMO_PASSWORD` | Sample customers' password (seed data, first start only) | Yes |
| `AWS_REGION`, `BEDROCK_MODEL_ID` | Amazon Bedrock model for recommendations | No |
| `TEST_DB_PASSWORD` | Test database password | Yes |
| `SERVER_PORT` | Port on AWS (default 5000) | No |

## API

| Method | Path | Story |
|---|---|---|
| GET | `/api/auth/session` | US-01 (is the browser logged in; also sets the CSRF cookie) |
| POST | `/api/auth/login` | US-01 |
| POST | `/api/auth/logout` | US-01 |
| GET | `/api/me` | US-01 / US-05 |
| GET | `/api/policies/mine` | US-02 |
| GET | `/api/products/available` | US-03 |
| POST | `/api/recommendations` | US-04 |
| GET | `/api/registrations/quote?productCode=` | US-05 |
| POST | `/api/policies` | US-05 |

Every path except `/api/auth/session` and `/api/auth/login` needs a logged-in session, and every POST needs the
CSRF token. Errors are RFC 9457 ProblemDetail JSON with a `code`:

| Status | Codes |
|---|---|
| 400 | `VALIDATION_FAILED`, `INVALID_DATE_OF_BIRTH`, `BAD_REQUEST` |
| 401 | `INVALID_CREDENTIALS`, `UNAUTHENTICATED` |
| 403 | `CSRF_TOKEN_INVALID`, `FORBIDDEN` |
| 404 | `PRODUCT_NOT_FOUND`, `CUSTOMER_NOT_FOUND` |
| 409 | `DUPLICATE_POLICY`, `DATA_CONFLICT` |
| 422 | `AGE_NOT_ELIGIBLE`, `PRODUCT_INACTIVE`, `INVALID_PRODUCT_SETUP`, `INVALID_POLICY_DATES` |
| 500 | `SERVER_ERROR` |

## Known limitations

| Priority | Limitation | Impact | Planned improvement |
|---|---|---|---|
| Future | FE-01 AI-assisted claim assessment and approval | Excluded from current scope | Build after FR-1 to FR-3 are accepted |
| P2 | Premium payments (FR-4) and policy search (FR-5) were removed from scope | Installment schedules are created but can't be paid or searched in the app | Re-introduce when the spec brings them back |
| P3 | Session cookie is not marked Secure because the AWS environment serves plain HTTP | Cookie could be read on an untrusted network | Add HTTPS (load balancer certificate) and set `server.servlet.session.cookie.secure=true` |
