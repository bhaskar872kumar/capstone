# Test catalog

Run all: `cd backend && mvn verify` (needs the `healthdekho_test` database). Every business rule has a passing (+)
and a failing (−) test. Business "today" in API tests is fixed at 2026-11-25. API tests log in through the real
Spring Security JDBC login and send a CSRF token on every POST.

Types: **U** unit · **R** repository (PostgreSQL) · **A** API (Spring Boot + MockMvc + Spring Security + transactions) · **AI** AI contract.

| Test id | Story | Type | ± | What it proves | File |
|---|---|---|---|---|---|
| AUTH_01 | US-01 | A | + | Valid login starts a session holding the logged-in customer | AuthApiTest |
| AUTH_02 | US-01 | A | − | Wrong password → 401 "Invalid email or password" | AuthApiTest |
| AUTH_03 | US-01 | A | − | Unknown email gives the same 401 (no account discovery) | AuthApiTest |
| AUTH_04 | US-01 | A | − | Protected endpoint without a session → 401 | AuthApiTest |
| AUTH_05 | US-01 | A | − | A change without the CSRF token → 403 | AuthApiTest |
| AUTH_06 | US-01 | A | + | /api/me returns only the logged-in customer | AuthApiTest |
| AUTH_07 | US-01 | A | − | Invalid email format → 400 with field error | AuthApiTest |
| AUTH_08 | US-01 | A | + − | Session check reports logged out, then logged in | AuthApiTest |
| AUTH_09 | US-01 | A | + | Logout ends the session | AuthApiTest |
| AUTH_10 | US-01 | A | − | Inactive customer can't log in (JDBC "enabled" column) | AuthApiTest |
| AUTH_11 | US-01 | A | + | Email is not case-sensitive | AuthApiTest |
| DASH_01 | US-02 | A | + | Dashboard shows the customer's own policy, never another's | DashboardAndProductsApiTest |
| DASH_02 | US-02 | A | − | Customer without policies gets an empty list | DashboardAndProductsApiTest |
| DASH_03 | US-02 | A | − | Dashboard needs a login | DashboardAndProductsApiTest |
| DISC_01 | US-03 | A | + | Registered product is excluded from available policies | DashboardAndProductsApiTest |
| DISC_02 | US-03 | A | − | Inactive product is excluded | DashboardAndProductsApiTest |
| DISC_03 | US-03 | A | + | Available list has no duplicates | DashboardAndProductsApiTest |
| AI_01 | US-04 | U | + | Valid AI answer is returned, saved with model metadata | RecommendationServiceTest |
| AI_02 | US-04 | U | − | Products the AI invented (and repeats) are dropped | RecommendationServiceTest |
| AI_03 | US-04 | U | − | AI timeout → rule-based fallback, saved as "fallback" | RecommendationServiceTest |
| AI_04 | US-04 | U | − | Age-ineligible products are never sent to or returned from AI | RecommendationServiceTest |
| AI_05 | US-04 | U | − | Future date of birth rejected, AI not called | RecommendationServiceTest |
| AI_06 | US-04 | U | − | AI error → fallback | RecommendationServiceTest |
| AI_07 | US-04 | U | − | No open products → empty result, AI not called | RecommendationServiceTest |
| AI_08 | US-04 | U | + | Fallback ranks by preferred type, term and cost | FallbackRankerTest |
| AI_09 | US-04 | U | + | Fallback limits results and handles an empty list | FallbackRankerTest |
| AI_10 | US-04 | U | + | Fallback suitability bands | FallbackRankerTest |
| AI_11 | US-04 | AI | + | Prompt has the customer inputs and only the given products | SpringAiRecommendationClientTest |
| AI_12 | US-04 | AI | + | JSON reply is read even inside code fences | SpringAiRecommendationClientTest |
| AI_13 | US-04 | AI | − | Unusable replies give no result (then fallback) | SpringAiRecommendationClientTest |
| AI_14 | US-04 | AI | − | No configured model gives no result (then fallback) | SpringAiRecommendationClientTest |
| AI_15 | US-04 | A | + | Recommendation saved against the customer, returned with item ids | RecommendationApiTest |
| AI_16 | US-04 | A | − | Missing goals → 400 with field error | RecommendationApiTest |
| AI_17 | US-04 | A | − | Future date of birth → 400 | RecommendationApiTest |
| REG_01 | US-05 | A | + | Registration saves the policy and all 40 installments with correct dates and total | RegistrationApiTest |
| REG_02 | US-05 | A | − | Duplicate registration → 409, no new row | RegistrationApiTest |
| REG_03 | US-05 | A | − | Age outside entry range → 422 with the allowed range | RegistrationApiTest |
| REG_04 | US-05 | A | − | Failure while saving the schedule rolls back the policy | RegistrationRollbackTest |
| REG_05 | US-05 | U | + − | Calculator: all four frequencies, spec sample values, bad terms, age | PolicyCalculatorTest |
| REG_06 | US-05 | A | − | Unknown product → 404 | RegistrationApiTest |
| REG_07 | US-05 | A | + | Registering from a recommendation marks the item selected | RegistrationApiTest |
| REG_08 | US-05 | A | + | Quote pre-populates customer and policy fields | RegistrationApiTest |
| REG_09 | US-05 | A | − | Quote flags an already-registered product | RegistrationApiTest |
| REG_10 | US-05 | A | − | Inactive product → 422 | RegistrationApiTest |
| REG_11 | US-05 | A | − | Missing product code → 400 | RegistrationApiTest |
| REG_12 | US-05 | A | − | Registration needs a login | RegistrationApiTest |
| REPO_01 | US-05 | R | − | Database blocks the same product twice for a customer | RepositoryConstraintTest |
| REPO_02 | US-05 | R | − | Database blocks a repeated installment number | RepositoryConstraintTest |
| REPO_03 | US-05 | R | − | Database blocks a paid amount above the scheduled amount | RepositoryConstraintTest |
| REPO_04 | all | R | + | Sample data is complete (3 customers, 240 installments) | RepositoryConstraintTest |
| REPO_05 | US-05 | R | + | Policy number sequence continues after the sample numbers | RepositoryConstraintTest |
| FLOW_01 | US-01..05 | A | + | Login → available → quote → register → dashboard → logout | FlowApiTest |

## Coverage

Report: `backend/target/site/jacoco/index.html` after `mvn verify`.
Targets (template slide 13): service line coverage ≥ 80%, critical branch coverage ≥ 75%.
The check runs on classes named `*Service`; set `haltOnFailure` to `true` in `pom.xml` once the report meets it.

Excluded from coverage, and why:

| Excluded | Reason |
|---|---|
| `HealthDekhoApplication` | Only starts Spring |
| `config/**` | Clock and route-forwarding setup, no business logic |
| `**/dto/**` | Records that only carry data |
