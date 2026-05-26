# Banking Application — Project Requirements

> **Course:** Code Generation 2025/2026 · Spring Boot REST API + JS Frontend  
> **Stack:** Java 21 · Spring Boot · H2/JPA · JWT · OpenAPI/SwaggerUI · JUnit · Vue.js or React  
> **Roles:** `CUSTOMER` · `EMPLOYEE`  
> **Status:** aligned with the adopted API implementation and the original assignment stories

---

## 1. Purpose And Source Of Truth

This document translates the original banking assignment into the project contract the team is now building against.

The original user stories remain the grading baseline. The implemented API has intentionally moved away from some early design sketches, so the accepted backend contract is:

- Authentication: `POST /auth/register`, `POST /auth/login`, `GET /auth/me`
- Customer administration: `/users`
- Account browsing, lookup, limits, and closure: `/accounts`
- Transfers, deposits, withdrawals, and transaction history: `/transactions`
- API documentation: `/swagger-ui/index.html` backed by `/openapi.yaml`

When this file and OpenAPI disagree, update both. The frontend must consume the REST API only and must not rely on database access or hardcoded seed data.

---

## 2. Developer / Infrastructure Requirements

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| D1 | Backend is built with Java and Spring Boot. | Backend code uses Spring Boot controllers, services, repositories, validation, security, and JPA entities. |
| D2 | Data is persisted in a relational database. | H2 is acceptable for the proof of concept. The schema supports users, customer profiles, accounts, and transactions. |
| D3 | Frontend is built with Vue.js or React. | Frontend communicates exclusively through REST endpoints. No direct database access. |
| D4 | API documentation is available. | SwaggerUI is available at `/swagger-ui/index.html` and loads `/openapi.yaml`. It documents all supported routes, query parameters, request bodies, response bodies, and error responses. |
| D5 | Security uses JWT. | Public routes are limited to registration, login, SwaggerUI, and OpenAPI. Protected routes require `Authorization: Bearer <token>`. Role-specific behavior is enforced server-side. |
| D6 | Tests support the implemented behavior. | Unit and functional tests cover authentication, account policies, transaction policies, controller behavior, and important DTO boundaries. Target assignment coverage remains 90%+. |
| D7 | The application can be deployed. | Backend can be hosted publicly, frontend can be hosted publicly, and SwaggerUI remains reachable on the deployed backend. |

---

## 3. Domain Model

### Roles

- `CUSTOMER`: can register, log in, view own accounts, search permitted account information, create own transactions, and view own transaction history.
- `EMPLOYEE`: can manage customers, view accounts system-wide, update account limits/status, create transactions on behalf of customers, and view transactions system-wide.

### Statuses And Types

| Concept | Values |
|---------|--------|
| Customer status | `PENDING`, `ACTIVE`, `CLOSED` |
| Account type | `CHECKING`, `SAVINGS` |
| Account status | `ACTIVE`, `CLOSED` |
| Transaction type | `TRANSFER`, `DEPOSIT`, `WITHDRAWAL` |

### Money And Currency

- All monetary values are EUR amounts.
- No currency conversion is supported.
- Amounts must be positive for transactions.
- Account and transaction amounts should use decimal-safe types, not floating point arithmetic.

### IBAN Format

Generated account numbers use the adopted project format:

`NLxxINHLxxxxxxxxxx`

Each generated IBAN must be unique. This differs from the original example `NLxxINHO0xxxxxxxxx`; the project implementation standard is `INHL` with a 10-digit generated suffix.

---

## 4. Authentication And Registration

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| A1 | Customers can register. | `POST /auth/register` accepts `email`, `password`, `firstName`, `lastName`, `bsn`, and `phoneNumber`. Email must be valid, password must meet the configured complexity rule, BSN must be 8 or 9 digits, and phone number must match the Dutch mobile format currently enforced by the API. Duplicate user data is rejected. |
| A2 | Registered customers start pending. | Registration creates a customer user/profile with status `PENDING` and no bank accounts. |
| A3 | Users can log in. | `POST /auth/login` accepts email and password and returns a token object plus current user data. |
| A4 | Pending customers can log in. | A `PENDING` customer receives a valid JWT. The frontend routes them to the pending welcome screen and blocks banking/ATM features. Login must not fail only because the customer is pending. |
| A5 | Current session can be rehydrated. | `GET /auth/me` returns current user identity, role, customer status, BSN, and phone number for authenticated users. |

---

## 5. Customer Requirements

### 5.1 Pending Customer Experience

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C1 | Pending customers see a limited interface. | After login, `PENDING` customers can view a basic welcome/pending page with their profile information and logout. They cannot create transactions or use ATM actions. |

### 5.2 Account Details

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C2 | Active customers can view their own accounts. | `GET /accounts` returns only the authenticated customer's accounts. Response data includes IBAN, account type, balance, absolute transfer limit, daily transfer limit, status, and creation time. |
| C3 | Customers can see a combined balance. | The frontend computes combined balance by summing active account balances returned from `GET /accounts`, unless a dedicated backend summary is added later. |
| C4 | Customers cannot access other customers' private account details. | Customer requests are scoped server-side to the authenticated user's own accounts. Customer-provided `userId` and `name` filters must not widen access. |

### 5.3 IBAN Lookup

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C5 | Customers can search for another customer's IBAN by name. | The adopted API uses `GET /accounts?name=<search>` for employee-wide lookup and scoped account search. If customer-facing public lookup is kept, it must expose only safe fields needed for transfer selection and must not expose balances or limits. |

> **Implementation note:** Customer name lookup returns safe transfer-target data only: IBAN, first name, and last name for active checking accounts owned by matching other customers. It does not expose balances, limits, or private account details.

### 5.4 Transfers

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C6 | Customers can transfer between their own accounts. | `POST /transactions` with `type: "TRANSFER"` requires `fromIban`, `toIban`, and `amount`. The customer must own the source account. Source and destination accounts must be active and different. |
| C7 | Customers can transfer to another customer's checking account. | The source account must belong to the authenticated customer. External customer transfers must target checking accounts. The backend must enforce ownership, account status, absolute floor limit, and daily outgoing limit. |
| C8 | Transfer records are persisted. | Successful transfers update both account balances and create a transaction record with source IBAN, destination IBAN, amount, type, description, timestamp, and initiating user. |

### 5.5 ATM

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C9 | Customers can use a mock ATM frontend. | ATM login reuses `POST /auth/login`. Pending customers are blocked by the frontend after reading returned status. |
| C10 | Customers can deposit cash. | ATM deposit uses `POST /transactions` with `type: "DEPOSIT"`, `toIban`, and `amount`. Deposit credits the destination account and records a transaction. Deposits do not consume outgoing transfer limits because no money leaves the customer account. |
| C11 | Customers can withdraw cash. | ATM withdrawal uses `POST /transactions` with `type: "WITHDRAWAL"`, `fromIban`, and `amount`. Withdrawal debits the source account, records a transaction, and enforces active account, source ownership, absolute floor limit, and daily outgoing limit. |

### 5.6 Transaction History

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| C12 | Customers can view their transaction history. | `GET /transactions` returns paginated transactions scoped to the authenticated customer. |
| C13 | Customers can filter transaction history. | Adopted filters are `iban`, `type`, `minAmount`, `maxAmount`, `page`, `size`, and `sort`. Exact amount filtering is represented by using the same value for `minAmount` and `maxAmount`. |

> **Implementation note:** The original assignment asks for date range filtering. The current API does not expose `startDate` or `endDate`; add those filters before claiming full coverage of the original transaction-filter story.

---

## 6. Employee Requirements

### 6.1 Customer Management

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| E1 | Employees can list customer users. | `GET /users` is employee-only and supports pagination plus optional `status` and `search` filters. `GET /users?status=PENDING` is the pending approval queue. |
| E2 | Employees can view a customer detail. | `GET /users/{id}` is employee-only and returns customer profile data, status, total balance, and account summaries. |
| E3 | Employees can update customer profile/status. | `PATCH /users/{id}` can update status, name fields, phone number, and activation limits. |
| E4 | Employees can approve a signup and create accounts. | Setting a pending customer to `ACTIVE` through `PATCH /users/{id}` creates one checking account and one savings account. `absoluteTransferLimit` and `dailyTransferLimit` in the same request are applied to both accounts; if omitted, configured defaults are used. |
| E5 | Employees can close/deactivate customers. | Setting customer status to `CLOSED` disables the customer profile from normal banking workflows. Frontend should hide banking actions for closed customers. |

### 6.2 Account Management

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| E6 | Employees can view all accounts. | `GET /accounts` is available to employees and supports `userId`, `type`, `status`, `iban`, `name`, pagination, and sorting. |
| E7 | Employees can update account limits. | `PATCH /accounts/{iban}` accepts `absoluteTransferLimit` and/or `dailyTransferLimit`. Values must be zero or greater. |
| E8 | Employees can close accounts. | `PATCH /accounts/{iban}` with `status: "CLOSED"` closes an account only when its balance is zero. Closed accounts cannot be used for transactions. |
| E9 | Employees can transfer between customer accounts. | Employees may create `TRANSFER` transactions using `POST /transactions` without owning the source account. Account status, absolute floor limit, and daily outgoing limit are still enforced. |

### 6.3 Transaction Oversight

| # | Requirement | Acceptance Criteria |
|---|-------------|---------------------|
| E10 | Employees can view all transactions. | `GET /transactions` returns paginated system-wide transactions for employees. |
| E11 | Employees can filter transactions. | Employee filters include `customerId`, `iban`, `type`, `minAmount`, `maxAmount`, `page`, `size`, and `sort`. |
| E12 | Employees can view a transaction detail. | `GET /transactions/{id}` returns a transaction when it exists. Customers may only access transaction details they initiated; employees can access all. |

---

## 7. Transaction Rules

| Type | Required fields | Balance effect | Limit behavior |
|------|-----------------|----------------|----------------|
| `TRANSFER` | `fromIban`, `toIban`, `amount`, `type` | Debits source and credits destination | Enforces source ownership for customers, active source/destination, different accounts, absolute floor limit, and daily outgoing limit |
| `DEPOSIT` | `toIban`, `amount`, `type` | Credits destination | Requires active destination account; does not enforce outgoing limits |
| `WITHDRAWAL` | `fromIban`, `amount`, `type` | Debits source | Enforces source ownership for customers, active source account, absolute floor limit, and daily outgoing limit |

### Limit Definitions

- `absoluteTransferLimit` is a per-account balance floor. A debit is rejected when `balance - amount` would be lower than this value.
- `dailyTransferLimit` is a per-source-account outgoing limit for the current calendar day. Prior `TRANSFER` and `WITHDRAWAL` transactions both count toward this limit.

---

## 8. Frontend Screen Requirements

The frontend should implement the workflow screens described in the project-management document, adjusted to the adopted API:

- Login
- Register
- Pending customer welcome
- Customer dashboard
- Customer account details
- Transfer money
- Customer transaction history and filters
- ATM login/menu/deposit/withdraw
- Employee dashboard
- Pending customer approvals using `GET /users?status=PENDING`
- Approve customer using `PATCH /users/{id}`
- Customer detail for employee using `GET /users/{id}`
- All accounts using `GET /accounts`
- Edit account limits/status using `PATCH /accounts/{iban}`
- All transactions using `GET /transactions`
- API documentation link to `/swagger-ui/index.html`

The customer IBAN lookup screen can use `GET /accounts?name=<search>` and should treat customer responses as safe transfer-target rows rather than full account detail rows.

---

## 9. Known Gaps To Resolve Before Final Submission

These are the remaining places where the implementation contract does not fully satisfy the original assignment wording:

| Gap | Why it matters | Recommended action |
|-----|----------------|--------------------|
| Date range transaction filters are not implemented. | Original story requires start/end date filtering. | Add `startDate` and `endDate` to `TransactionFilterParams`, repository query, OpenAPI, tests, and frontend filters. |
| OpenAPI must stay synchronized with code. | API design grade depends on conformity to implementation. | Update `/src/main/resources/openapi.yaml` whenever controllers/DTOs change. |

---

## 10. Testing And Delivery

| Area | Minimum Evidence |
|------|------------------|
| Authentication | Register, login, pending login, JWT-protected access, invalid credentials. |
| Customer approval | Pending customer list, activation, checking/savings account creation, default/custom limits. |
| Account management | Employee account listing/filtering, limit updates, account closure with zero/non-zero balance cases. |
| Transactions | Transfer, deposit, withdrawal, ownership checks, inactive account checks, absolute limit checks, daily limit checks. |
| Transaction history | Customer-scoped history, employee-wide history, amount/type/IBAN/customer filters, pagination. |
| API docs | SwaggerUI loads and documents the implemented endpoints. |
| Frontend | Role-based routing, pending flow, customer workflows, employee workflows, API error handling. |

Before final submission, run the backend test suite, verify SwaggerUI locally, and verify frontend flows against the deployed or local backend.

---

## 11. Grading Criteria Summary

| Component | Weight | Graded on |
|-----------|--------|-----------|
| Code Assessment (`1918IN241D`) | 90% | Feature completeness, code quality, Spring framework adherence, consistency, individual explanation, test validity, test coverage, and functional complexity. Frontend can contribute as optional extra evidence. |
| API Design (`1918IN241B`) | 10% | Feature completeness, RESTful design, legibility of OpenAPI/SwaggerUI, consistency, and conformity to implementation. |
| Process Dossier (`1919IN241E`) | 0% | Cooperation agreement and retro evaluation form. Pass/fail. |

### Code Quality Rubric Dimensions

- Readability and maintainability
- Robustness and secure handling of edge cases
- Performance appropriate for the project scale
- Correctness against this requirements contract and the original assignment stories
- Professional, demonstrable product quality

---

*Last updated to reflect the adopted API implementation and the project-management descriptions.*
