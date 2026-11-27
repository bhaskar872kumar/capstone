# HealthDekho – problem statement

Paste the full problem statement from Mettl here (sections 1–5, with all tables and sample data).
The code and tests refer to it by section number (FR-1 … FR-3, 5.1 … 5.5).

Spec update: **FR-4 (premium payment management) and FR-5 (customer policy search) are removed from scope.**
Authentication uses Spring Security's JDBC support against the customer table.

Current scope:

| Story | Spec | What it covers |
|---|---|---|
| US-01 | FR-1 A | Customer login with Spring Security JDBC authentication |
| US-02 | FR-1 B | Dashboard of the customer's own policies |
| US-03 | FR-1 C | Active products the customer hasn't registered |
| US-04 | FR-2 | AI recommendations with Spring AI, saved per customer |
| US-05 | FR-3 | Policy registration with calculated dates, totals and installment schedule |

Out of scope: US-06 premium payments (FR-4, removed), US-07 policy search (FR-5, removed),
US-08 AI claim assessment (FE-1, future).
