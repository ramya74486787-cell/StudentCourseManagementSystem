# Equipment Loan & Approval System

Spring Boot backend for requesting, approving, and returning borrowed equipment,
with a two-stage approval workflow (supervisor → store keeper) and accurate
stock tracking under concurrent access.

## Stack

- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security (JWT, stateless)
- MySQL (runtime), H2 in-memory (tests only)
- Lombok

## Running it

1. Have a MySQL server running locally (or point `DB_USERNAME` / `DB_PASSWORD` /
   the URL in `application.yml` at whatever instance you want). The schema is
   created automatically (`ddl-auto: update`) — no manual DB setup needed
   beyond the server existing.
2. `mvn spring-boot:run`
3. API is on `http://localhost:8080`.

Environment variables (all optional, sensible dev defaults are baked in):

| Variable | Default |
|---|---|
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |
| `JWT_SECRET` | dev key baked into `application.yml` |
| `JWT_EXPIRATION_MS` | `86400000` (24h) |

## Running the tests

```
mvn test
```

Tests run against an in-memory H2 database (see `src/test/resources/application-test.yml`),
so no MySQL instance is needed to run the test suite, including the concurrency test.

## API overview

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>`.

| Method | Path | Who | What |
|---|---|---|---|
| POST | `/api/auth/register` | anyone | create an account (pick a role) |
| POST | `/api/auth/login` | anyone | get a JWT |
| POST | `/api/equipment` | STORE_KEEPER | add equipment |
| GET | `/api/equipment?page=&size=` | any authenticated | paginated equipment list |
| POST | `/api/loan-requests` | EMPLOYEE | submit a request |
| GET | `/api/loan-requests/my?page=&size=` | any authenticated | your own requests only |
| GET | `/api/loan-requests/pending-my-action` | SUPERVISOR / STORE_KEEPER | requests sitting at your stage |
| POST | `/api/loan-requests/{id}/approve` | SUPERVISOR / STORE_KEEPER | approve at your stage |
| POST | `/api/loan-requests/{id}/reject` | SUPERVISOR / STORE_KEEPER | reject (requires `comments`) |
| POST | `/api/loan-requests/{id}/return` | the original requester | mark returned, restock |
| GET | `/api/loan-requests/search?status=&category=&from=&to=` | any authenticated | filtered search |
| GET | `/api/dashboard` | any authenticated | totals, per-stage backlog, top items |

`register` takes a `role` field directly (`EMPLOYEE` / `SUPERVISOR` / `STORE_KEEPER`)
so you can spin up test accounts for every role without a separate admin flow —
there was no user-management endpoint requested in the brief, so this is the
simplest way to get all three roles into the system.

## How the workflow maps to the data model

`LoanRequest.currentStage` is a pointer — "who needs to act on this next" —
and gets overwritten as the request moves. `ApprovalStage` rows are the
append-only log: one new row is written every time someone actually makes a
decision, and old rows are never touched again. `LoanRequestResponse` returns
the full `ApprovalStage` history alongside the current pointer, so you can see
both "where is this now" and "how did it get here."

Flow:

```
PENDING/SUPERVISOR_REVIEW
   → supervisor approves → PENDING/STORE_KEEPER_REVIEW
   → supervisor rejects  → REJECTED/REJECTED (dead end, rejectionReason set)

PENDING/STORE_KEEPER_REVIEW
   → store keeper approves → stock reduced → APPROVED/COMPLETED
   → store keeper rejects  → REJECTED/REJECTED

APPROVED/COMPLETED
   → requester marks returned → RETURNED/COMPLETED, stock restored
```

A rejected request is never revived — the employee submits a brand new one.

## The concurrency problem (the part that actually matters)

**The scenario:** two employees each have a request for the same equipment
sitting at `STORE_KEEPER_REVIEW`. Only one unit is left. The store keeper's
UI shows both as approvable (because it does — the stock check passes for
both when each page loaded). If two `approve` calls land on the server at
nearly the same instant, a naive implementation would:

1. Thread A reads `availableUnits = 1`, passes the `>= quantity` check.
2. Thread B reads `availableUnits = 1`, passes the `>= quantity` check.
3. Thread A writes `availableUnits = 0`.
4. Thread B writes `availableUnits = 0` (based on B's own read of `1 - 1`).

Both requests get approved, stock is now `0` but two units were "handed out"
against one — the classic lost-update problem. A single `SELECT` + `UPDATE`
with no locking cannot detect this on its own.

**Why optimistic locking (`@Version`) instead of pessimistic locking:**

- Approvals aren't high-frequency, high-contention operations (unlike, say, a
  flash-sale checkout), so paying the cost of holding a row lock for the
  duration of each approval isn't worth it — most of the time there's no
  contention at all, and a lock would only ever protect against the rare case
  when there is.
- Optimistic locking doesn't hold any database locks or connections open
  across the "check stock → decide → write" logic, so it also doesn't create
  any risk of a deadlock between two approvals touching the same equipment
  from different requests in different orders.
- It's a natural fit here: JPA/Hibernate does the version-comparison
  automatically via `@Version` on `Equipment.version` — every `UPDATE`
  generated for that entity implicitly becomes
  `UPDATE equipment SET available_units = ?, version = ? WHERE id = ? AND version = ?`.
  If zero rows are affected (because someone else already bumped the
  version), Hibernate throws `ObjectOptimisticLockingFailureException`
  for us — no hand-rolled `WHERE` clause or manual version bookkeeping needed.

**How it's implemented (`LoanRequestService#reduceStockOrFail`):**

1. Re-fetch the `Equipment` row fresh at the moment of approval (not the
   possibly-stale copy attached to the `LoanRequest`), so the check is
   against the latest known stock.
2. Check `availableUnits >= quantity`. If not, fail fast with
   `InsufficientStockException` (409) — no version conflict needed, the
   stock is just genuinely gone.
3. Decrement and call `equipmentRepository.saveAndFlush(equipment)` —
   deliberately `saveAndFlush`, not `save`, so the `UPDATE` (and therefore
   the version check) happens synchronously inside this method, not lazily
   at transaction commit. That matters: if we let it flush at commit time,
   the exception would surface outside our `try/catch`, past the point where
   we can turn it into a clean, meaningful error.
4. Catch `OptimisticLockingFailureException` and re-throw as our own
   `StockConflictException`, which `GlobalExceptionHandler` maps to
   **HTTP 409** with a human-readable message — not a 500, and not a silent
   double-approval.

Net effect for the "last unit, two requests" scenario: whichever transaction
commits its `UPDATE` first wins and gets `APPROVED`. The second transaction's
version check fails (the row's version no longer matches what it read), it
gets a 409, and its `LoanRequest` stays at `STORE_KEEPER_REVIEW` — the store
keeper can act on it again later, and if stock is genuinely gone by then
they'll get a normal `InsufficientStockException` instead of a conflict.
Stock never goes negative and is never double-decremented.

`return` (restocking) goes through the same `saveAndFlush` + catch pattern
for consistency, even though the "add units back" case is much less likely to
collide in practice.

**Test:** `src/test/java/com/equipment/loan/ConcurrentStockUpdateTest.java`
sets up one piece of equipment with exactly 1 unit available and two loan
requests already sitting at `STORE_KEEPER_REVIEW`, then fires both `approve`
calls from two threads released at the same instant via a `CountDownLatch`.
It asserts:

- exactly one approval succeeds and exactly one is rejected as a conflict,
- no unexpected exception types leak out (no accidental 500s),
- final `availableUnits` is exactly `0`, never negative, never double-spent.

## Other notes

- Controllers never return entities directly — every response goes through a
  `*Response` DTO, so passwords never leave the service layer and Hibernate
  proxies never leak into JSON.
- `approve` + `reject` + stock update are single `@Transactional` methods, so
  a request can't end up "half approved" (e.g. `ApprovalStage` written but
  stock not reduced, or vice versa) if something fails partway through.
- Ownership of "my requests" and "mark returned" is derived entirely from the
  JWT (`CurrentUserProvider`), never from a path parameter, so there's no way
  to view or return someone else's request by editing an id in the URL.
- Role vs. stage validation lives in `LoanRequestService#assertCanAct`: a
  supervisor can't approve a request that's already moved past them to the
  store keeper, and an employee can't hit approve/reject at all (blocked at
  both the `@PreAuthorize` layer and, redundantly, in the service).
