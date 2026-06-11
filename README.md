# 🛒 FirstClub Membership Program — Backend System

> A production-grade membership subscription service with tiered benefits, configurable criteria, and concurrency-safe operations built with Java 17 + Spring Boot 3.2.

---

## 📋 Table of Contents

- [Problem Statement](#-problem-statement)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Entity Design & Rationale](#-entity-design--rationale)
- [Design Patterns Used](#-design-patterns-used)
- [Concurrency Handling](#-concurrency-handling)
- [API Reference](#-api-reference)
- [Request Flow — End to End](#-request-flow--end-to-end)
- [Running the Project](#-running-the-project)
- [Demo Walkthrough](#-demo-walkthrough)
- [Key Design Decisions Summary](#-key-design-decisions-summary)

---

## 🎯 Problem Statement

Design a backend system for a **Membership Program** for FirstClub that supports:

- Monthly, Quarterly, and Yearly subscription plans
- Tiered membership (Silver, Gold, Platinum) with configurable benefits
- Automatic tier progression based on order activity (order count, order value, cohort)
- User actions: subscribe, upgrade tier, downgrade tier, cancel, track membership
- Concurrency-safe operations
- Extensible and modular design

---

## 🛠 Tech Stack

| Technology | Version | Why |
|-----------|---------|-----|
| Java | 17 | LTS, records, sealed classes, modern streams |
| Spring Boot | 3.2.0 | Production-ready auto-configuration, embedded Tomcat |
| Spring Data JPA | 3.2.0 | Repository abstraction, JPQL, locking annotations |
| Hibernate | 6.3 | ORM with optimistic/pessimistic lock support |
| H2 Database | Runtime | In-memory DB for demo; swap with MySQL/PostgreSQL in prod |
| Lombok | Latest | Eliminates boilerplate (Builder, Getter, Setter) |
| SpringDoc OpenAPI | 2.3.0 | Auto-generates Swagger UI from annotations |
| Spring Validation | 3.2.0 | Request body validation via `@Valid` |

---

## 🏗 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client / API Consumer                 │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP Request
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   MembershipController                       │
│        (REST layer — validates input, routes to service)     │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┴────────────────┐
          ▼                                ▼
┌──────────────────┐            ┌──────────────────────┐
│ MembershipService│            │ TierEvaluationService│
│ (business logic) │            │ (strategy evaluation)│
└────────┬─────────┘            └──────────┬───────────┘
         │                                 │
         │                    ┌────────────┴──────────────┐
         │                    │  TierCriteriaStrategyFactory│
         │                    │  ┌──────────────────────┐  │
         │                    │  │OrderCountStrategy    │  │
         │                    │  │OrderValueStrategy    │  │
         │                    │  │CohortStrategy        │  │
         │                    │  └──────────────────────┘  │
         │                    └───────────────────────────┘
         │
┌────────┴──────────────────────────────────────────┐
│                  Repository Layer                  │
│  MembershipPlanRepo  │  MembershipTierRepo        │
│  UserMembershipRepo  │  MembershipAuditLogRepo    │
└────────┬──────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────────────┐
│               H2 / PostgreSQL Database             │
│  membership_plans  │  membership_tiers            │
│  tier_benefits     │  tier_criteria               │
│  user_memberships  │  membership_audit_logs       │
└───────────────────────────────────────────────────┘
```

---

## 📐 Entity Design & Rationale

### Why these 7 entities?

The core design decision was to **separate concerns at the data level**. Benefits and criteria are NOT hardcoded — they are stored as entities so they can be changed without a code deployment.

---

### `MembershipPlan`
```
id | planType (MONTHLY/QUARTERLY/YEARLY) | name | price | durationDays | description | active
```

**Why:** Plans define the *billing cycle and price*. They are intentionally decoupled from tiers — a user can be on a MONTHLY plan with a PLATINUM tier. Plan controls *when* you pay; Tier controls *what* you get.

**Decision:** `planType` is `UNIQUE` in DB — prevents accidental duplicate plans.

---

### `MembershipTier`
```
id | tierLevel (SILVER/GOLD/PLATINUM) | name | description | tierOrder | active
```

**Why:** `tierOrder` (1, 2, 3) is the key field that enables upgrade/downgrade validation without string comparisons. The `TierLevel` enum exposes `isHigherThan()` and `isLowerThan()` methods that use `tierOrder` internally.

**Decision:** Tier and Plan are separate entities. This allows future scenarios like:
- *"Only YEARLY plan users can access PLATINUM tier"* — add a `planEligibility` field to `MembershipTier`
- *"Free tier with no plan"* — just create a MONTHLY plan with price=0

---

### `TierBenefit`
```
id | tier_id (FK) | benefitType (enum) | benefitValue (String) | description
```

**Why:** Benefits are stored as key-value pairs per tier. This means the product team can change *"Gold tier gives 10% discount"* to *"12% discount"* by updating a DB row — **no code change, no redeployment**.

**Decision:** `benefitValue` is a `String` intentionally. Different benefit types have different value types:
- `FREE_DELIVERY` → `"true"` / `"false"`
- `DISCOUNT_PERCENT` → `"10"`
- `FASTER_DELIVERY_HOURS` → `"24"`

The consumer (frontend/downstream service) parses the value based on `benefitType` context. This trades type-safety for maximum flexibility.

**Constraint:** `UNIQUE(tier_id, benefit_type)` — a tier cannot have duplicate benefit types.

---

### `TierCriteria`
```
id | tier_id (FK) | criteriaType (ORDER_COUNT/ORDER_VALUE/COHORT) | threshold | cohortValue | description
```

**Why:** Eligibility rules for automatic tier promotion are stored in the DB. This means:
- *"Raise GOLD threshold from 5 orders to 10 orders"* → `UPDATE tier_criteria SET threshold=10 WHERE ...`
- No code change needed.

**Decision:** Multiple criteria per tier use **OR semantics** — satisfying ANY ONE criteria qualifies the user. This is more practical than AND semantics for retail use cases (a high-value customer who placed 2 large orders should get Gold, even without hitting 5 orders).

**Decision:** `threshold` and `cohortValue` are separate nullable fields. `ORDER_COUNT`/`ORDER_VALUE` use `threshold`; `COHORT` uses `cohortValue`. This avoids a generic `value: String` field that would be harder to query and validate.

---

### `UserMembership`
```
id | userId | plan_id (FK) | tier_id (FK) | status | startDate | endDate | autoRenew | version | createdAt | updatedAt
```

**Why `@Version` field:** This is the **optimistic locking** key. Every time a `UserMembership` row is updated, Hibernate increments `version`. If two threads try to update the same row simultaneously, the second one gets an `OptimisticLockException` — preventing silent data corruption.

**Why `userId` is a String (not FK to a User table):** This service is designed as a **microservice**. It doesn't own the User domain. `userId` is an external identifier from the User Service. This keeps the Membership Service independently deployable and testable.

**Decision — `startDate`/`endDate` are `LocalDate` not `LocalDateTime`:** Membership validity is day-granular, not minute-granular. Using `LocalDate` avoids timezone issues.

**Decision — Composite Indexes:** Strategic composite indexes are used instead of low-cardinality standalone indexes:
- `(userId, status)` — covers the primary query `WHERE user_id = ? AND status = 'ACTIVE'` in a single index scan
- `(status, endDate)` — covers the scheduler query `WHERE status = 'ACTIVE' AND end_date < today`

A standalone index on `status` (only 4 possible values) would be nearly useless — the query optimizer would ignore it and perform a full table scan.

---

### `ActiveUserMembership`
```
id (user_id PK) | membershipId
```

**Why:** Pessimistic locks (`SELECT ... FOR UPDATE`) can only lock **existing rows**. When a user subscribes for the first time, there is no row to lock — two concurrent subscribe requests across instances can both pass the duplicate check and create two ACTIVE memberships. This shadow table solves it: on subscribe, we `INSERT INTO active_user_memberships(user_id)`. The primary key constraint on `user_id` ensures only one concurrent request succeeds; the second gets a `DataIntegrityViolationException` (mapped to HTTP 409 Conflict). On cancel/expire, the row is deleted.

**Decision:** This is paired with a partial unique index (`CREATE UNIQUE INDEX ... ON user_memberships(user_id) WHERE status = 'ACTIVE'`) created via `data.sql` (since JPA's `@Index` doesn't support `WHERE` clauses). This provides a database-level guarantee of one-active-membership-per-user, even across multiple horizontally-scaled instances.

---

### `MembershipAuditLog`
```
id | userId | action | previousState | newState | remarks | timestamp
```

**Why:** Every state change (SUBSCRIBED, UPGRADED, DOWNGRADED, CANCELLED, TIER_AUTO_UPGRADED) is immutably logged. This serves:
- **Debugging** — *"Why is this user on PLATINUM?"*
- **Compliance** — full history of billing-related changes
- **Analytics** — how many users upgrade within 30 days of subscribing?

**Decision:** This entity has no setters and no `@PreUpdate` — it is **append-only**. Once a log entry is created, it is never modified.

---

## 🧩 Design Patterns Used

### 1. Strategy Pattern — Tier Criteria Evaluation

**Problem:** We have 3 types of criteria (ORDER_COUNT, ORDER_VALUE, COHORT), each with different evaluation logic. Adding a 4th type (e.g., REFERRAL_COUNT) should not require changing existing code.

**Solution:** Each criteria type maps to a `TierCriteriaStrategy` implementation.

```
TierCriteriaStrategy (interface)
    ├── OrderCountCriteriaStrategy  → handles ORDER_COUNT
    ├── OrderValueCriteriaStrategy  → handles ORDER_VALUE
    └── CohortCriteriaStrategy      → handles COHORT
```

**Interface Contract:**
```java
public interface TierCriteriaStrategy {
    CriteriaType getType();
    boolean evaluate(TierCriteria criteria, TierEvaluationContext context);
}
```

**Why:** Open/Closed Principle — open for extension (new strategy), closed for modification (no changes to `TierEvaluationService`).

---

### 2. Factory Pattern — Strategy Resolution

**Problem:** Given a `CriteriaType` from the database, how do we get the right strategy at runtime without a giant `if-else` or `switch` block?

**Solution:** `TierCriteriaStrategyFactory` auto-discovers all `TierCriteriaStrategy` beans via Spring DI and builds a `Map<CriteriaType, TierCriteriaStrategy>`.

```java
public TierCriteriaStrategyFactory(List<TierCriteriaStrategy> strategies) {
    this.strategyMap = strategies.stream()
        .collect(Collectors.toMap(TierCriteriaStrategy::getType, Function.identity()));
}
```

**Why:** Zero-configuration factory. Adding a new strategy = just create a new `@Component` class. The factory picks it up automatically on startup.

---

### 3. Repository Pattern

**Problem:** Business logic should not be coupled to database queries.

**Solution:** Spring Data JPA repositories abstract all data access. The service layer only calls repository methods — it has no SQL or JPQL awareness.

**Special addition — Pessimistic Locking in Repository:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT um FROM UserMembership um WHERE um.userId = :userId AND um.status = :status")
Optional<UserMembership> findByUserIdAndStatusWithLock(...);
```

**Why:** The repository is the right place to declare locking strategy — not the service layer. This keeps the service clean and makes the locking behavior explicitly visible at the data access layer.

---

### 4. Builder Pattern (via Lombok)

All entities and DTOs use `@Builder`. This gives:
- Immutable-style construction with named parameters
- No telescoping constructors
- Easy to read at call sites: `UserMembership.builder().userId("u1").plan(plan).build()`

---

### 5. Template Method (ApiResponse Wrapper)

Every API response is wrapped in:
```java
ApiResponse<T> {
    boolean success;
    String message;
    T data;
    LocalDateTime timestamp;
}
```

**Why:** Consistent response envelope across all endpoints. The client always knows where to find data, error messages, and timestamp — regardless of which endpoint they call.

---

## 🔒 Concurrency Handling

Four layers of concurrency protection are used, each solving a different problem.

---

### Layer 1: Pessimistic Locking (DB-level)
**Where:** `UserMembershipRepository.findByUserIdAndStatusWithLock()`

**When used:** Subscribe, upgrade, downgrade, cancel, evaluate-tier — any **write** operation.

**How it works:**
```sql
SELECT * FROM user_memberships
WHERE user_id = 'user1' AND status = 'ACTIVE'
FOR UPDATE;  -- DB row is locked until transaction commits
```

**Why:** Prevents two simultaneous requests (e.g., two upgrade calls) from both reading the same membership state and both writing conflicting updates. The second request waits until the first transaction completes.

**Scenario prevented:**
```
Thread A: reads user1 membership (SILVER) → begins upgrade to GOLD
Thread B: reads user1 membership (SILVER) → begins upgrade to PLATINUM
Thread A: saves GOLD ✅
Thread B: saves PLATINUM ✅ ← without lock, both succeed, last write wins (data corruption)
With lock: Thread B waits → reads updated state (GOLD) → upgrade to PLATINUM succeeds correctly
```

---

### Layer 2: Optimistic Locking (@Version)
**Where:** `UserMembership` entity has a `@Version Long version` field.

**When it helps:** If two threads bypass the pessimistic lock (different entry points), Hibernate's optimistic lock acts as the last line of defense.

**How it works:**
```sql
UPDATE user_memberships
SET tier_id = 2, version = 2     -- Hibernate increments version
WHERE id = 1 AND version = 1;    -- Only succeeds if version hasn't changed
```

If `version` was already updated by another thread, the `WHERE version=1` matches 0 rows → Hibernate throws `OptimisticLockException` → caught by `GlobalExceptionHandler` → returns HTTP 409 Conflict with *"Please retry your request"*.

---

### Layer 3: Distributed-Safe Duplicate Subscribe Guard

**Problem:** `SELECT ... FOR UPDATE` (pessimistic lock) only locks **existing rows**. If no membership exists yet (first-time subscriber), there is nothing to lock — two concurrent requests (even across different instances) can both see "no existing membership" and both INSERT, creating duplicates.

**Solution — Shadow Table (`ActiveUserMembership`) + Partial Unique Index:**
```java
// Step 1: Application-level check (with pessimistic lock if row exists)
membershipRepository.findByUserIdAndStatusWithLock(userId, MembershipStatus.ACTIVE)
    .ifPresent(existing -> {
        throw new MembershipException("User already has an active membership.");
    });

// Step 2: Shadow table insert (PK constraint prevents duplicates across instances)
activeUserMembershipRepository.save(new ActiveUserMembership(userId, membership.getId()));
// → Second concurrent INSERT throws DataIntegrityViolationException → HTTP 409 Conflict

// On Cancel/Expire:
activeUserMembershipRepository.deleteByUserId(userId);
```

This is further reinforced by a partial unique index in `data.sql`:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_active_per_user
    ON user_memberships(user_id)
    WHERE status = 'ACTIVE';
```

**Scenario prevented (multi-instance):**
```
Instance 1: SELECT ... FOR UPDATE WHERE userId='user1' → empty (nothing to lock)
Instance 2: SELECT ... FOR UPDATE WHERE userId='user1' → empty (nothing to lock)
Instance 1: INSERT INTO active_user_memberships('user1') ✅
Instance 2: INSERT INTO active_user_memberships('user1') ❌ PK violation → 409 Conflict
```

---

### Layer 4: Distributed Scheduler Lock (ShedLock)

**Problem:** `@Scheduled` fires on **every** running instance simultaneously. With 3 instances and 100 expired memberships, each membership would be processed 3 times.

**Solution — ShedLock with JDBC provider:**
```java
@Scheduled(cron = "0 0 0 * * ?")
@SchedulerLock(
    name           = "processExpiredMemberships",
    lockAtLeastFor = "PT1M",   // hold lock >= 1 min (prevents rapid re-trigger)
    lockAtMostFor  = "PT10M"   // force-release after 10 min even if instance crashes
)
public void processExpiredMemberships() { ... }
```

ShedLock uses a `shedlock` database table (created in `data.sql`) with a primary key constraint on the lock name. All instances attempt to INSERT simultaneously — only one succeeds (PK constraint), that instance runs the job, the others skip silently. The lock auto-releases after `lockAtMostFor` even if the winning instance crashes.

---

## 📡 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/membership/plans` | Get all active plans + tiers with benefits |
| `POST` | `/api/v1/membership/subscribe` | Subscribe to a plan and tier |
| `GET` | `/api/v1/membership/status/{userId}` | Get current membership status |
| `PUT` | `/api/v1/membership/upgrade` | Manually upgrade tier |
| `PUT` | `/api/v1/membership/downgrade` | Manually downgrade tier |
| `PUT` | `/api/v1/membership/cancel/{userId}` | Cancel active membership |
| `POST` | `/api/v1/membership/evaluate-tier` | Auto-evaluate tier based on order data |
| `GET` | `/api/v1/membership/history/{userId}` | Get full audit trail |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**H2 Console:** `http://localhost:8080/h2-console`

---

## 🔄 Request Flow — End to End

### Flow 1: GET /api/v1/membership/plans

```
Client
  │
  │ GET /api/v1/membership/plans
  ▼
MembershipController.getPlansAndTiers()
  │
  │ No auth/lock needed — read-only
  ▼
MembershipServiceImpl.getPlansAndTiers()
  │
  ├─ planRepository.findByActiveTrue()
  │    └─ SELECT * FROM membership_plans WHERE active = true
  │
  ├─ tierRepository.findByActiveTrueOrderByTierOrderAsc()
  │    └─ For each tier → lazily loads tier_benefits + tier_criteria
  │
  ├─ Maps each plan → PlanResponse (DTO)
  ├─ Maps each tier → TierResponse (DTO)
  │    └─ benefits → Map<BenefitType, String>
  │    └─ criteria → List<CriteriaResponse>
  │
  └─ Returns PlansAndTiersResponse wrapped in ApiResponse<T>

Response: HTTP 200 with plans[] + tiers[] + benefits + criteria
```

---

### Flow 2: POST /api/v1/membership/subscribe

```
Client
  │
  │ POST /subscribe { userId, planType, tierLevel? }
  ▼
MembershipController.subscribe()
  │
  │ @Valid validates: userId not blank, planType not null
  ▼
MembershipServiceImpl.subscribe()
  │
  ├─ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  │    └─ SELECT ... FOR UPDATE
  │    └─ If found → throw MembershipException(409) "Already has active membership"
  │
  ├─ activeUserMembershipRepository.save(userId)
  │    └─ PK constraint prevents duplicate ACTIVE across instances
  │
  ├─ Build UserMembership:
  │    startDate = today
  │    endDate   = today + plan.durationDays
  │    status    = ACTIVE
  │    autoRenew = true
  │    version   = 0  (first version)
  │
  ├─ membershipRepository.save(membership)
  │    └─ INSERT INTO user_memberships (...)
  │
  ├─ auditLogRepository.save(log)
  │    └─ INSERT INTO membership_audit_logs (action=SUBSCRIBED, ...)
  │
  └─ Returns UserMembershipResponse (plan + tier + benefits + dates)

Response: HTTP 201 Created
```

---

### Flow 3: PUT /api/v1/membership/upgrade

```
Client
  │
  │ PUT /upgrade { userId, targetTier, reason }
  ▼
MembershipController.upgradeTier()
  │
  │ @Valid validates: userId not blank, targetTier not null
  ▼
MembershipServiceImpl.upgradeTier()
  │
  ├─ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  │    └─ SELECT ... FOR UPDATE
  │    └─ If not found → throw ResourceNotFoundException(404)
  │
  ├─ tierRepository.findByTierLevel(targetTier)
  │    └─ If not found → throw ResourceNotFoundException(404)
  │
  ├─ Validation: targetTier.isHigherThan(currentTier)?
  │    └─ Uses tierOrder: GOLD(2) > SILVER(1) → valid
  │    └─ If not higher → throw MembershipException(400) "Cannot upgrade"
  │
  ├─ membership.setTier(targetTier)
  │    └─ startDate, endDate UNCHANGED (period is preserved)
  │    └─ version auto-incremented by Hibernate
  │
  ├─ membershipRepository.save(membership)
  │    └─ UPDATE user_memberships SET tier_id=?, version=? WHERE id=? AND version=?
  │
  ├─ auditLogRepository.save(log)
  │    └─ action=UPGRADED, previousState="tier=SILVER", newState="tier=GOLD"
  │
  └─ Returns updated UserMembershipResponse

Response: HTTP 200 OK with new tier benefits
Note: Billing period (startDate/endDate) is NOT reset — user keeps remaining days
```

---

### Flow 4: POST /api/v1/membership/evaluate-tier

```
Client (triggered by Order Service after each order)
  │
  │ POST /evaluate-tier { userId, orderCount, totalOrderValue, userCohort }
  ▼
MembershipController.evaluateTier()
  ▼
MembershipServiceImpl.evaluateAndUpdateTier()
  │
  ├─ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  │    └─ If not found → throw ResourceNotFoundException(404)
  │
  ├─ Build TierEvaluationContext:
  │    { userId, orderCount=7, totalOrderValue=6000, userCohort="STANDARD" }
  │
  ├─ TierEvaluationService.evaluateBestTier(context)
  │    │
  │    ├─ Load all active tiers ordered DESCENDING by tierOrder
  │    │    [PLATINUM(3), GOLD(2), SILVER(1)]
  │    │
  │    ├─ For PLATINUM (tierOrder=3):
  │    │    Load criteria: [ORDER_COUNT>=20, ORDER_VALUE>=20000, COHORT=PREMIUM]
  │    │    │
  │    │    ├─ strategyFactory.resolve(ORDER_COUNT) → OrderCountCriteriaStrategy
  │    │    │    evaluate: 7 >= 20 → FALSE ❌
  │    │    ├─ strategyFactory.resolve(ORDER_VALUE) → OrderValueCriteriaStrategy
  │    │    │    evaluate: 6000 >= 20000 → FALSE ❌
  │    │    └─ strategyFactory.resolve(COHORT) → CohortCriteriaStrategy
  │    │         evaluate: "STANDARD" == "PREMIUM" → FALSE ❌
  │    │    Result: PLATINUM not qualified ❌
  │    │
  │    ├─ For GOLD (tierOrder=2):
  │    │    Load criteria: [ORDER_COUNT>=5, ORDER_VALUE>=5000]
  │    │    │
  │    │    ├─ strategyFactory.resolve(ORDER_COUNT) → OrderCountCriteriaStrategy
  │    │    │    evaluate: 7 >= 5 → TRUE ✅  ← OR logic stops here
  │    │    Result: GOLD qualified ✅
  │    │
  │    └─ Returns GOLD tier
  │
  ├─ Compare: bestTier(GOLD) vs currentTier(SILVER)?
  │    → Different → update!
  │
  ├─ membership.setTier(GOLD)
  ├─ membershipRepository.save(membership)
  │    └─ UPDATE user_memberships SET tier_id=2, version=? ...
  │
  ├─ auditLogRepository.save(log)
  │    └─ action=TIER_AUTO_UPGRADED, previousState="tier=SILVER", newState="tier=GOLD..."
  │
  └─ Returns updated UserMembershipResponse

Response: HTTP 200 OK with new tier
```

---

### Flow 5: PUT /api/v1/membership/cancel/{userId}

```
Client
  │
  │ PUT /cancel/user1
  ▼
MembershipController.cancelMembership()
  ▼
MembershipServiceImpl.cancelMembership()
  │
  ├─ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  │    └─ If not found → throw ResourceNotFoundException(404)
  │
  ├─ membership.setStatus(CANCELLED)
  │    └─ plan, tier, startDate, endDate unchanged
  │
  ├─ activeUserMembershipRepository.deleteByUserId(userId)
  ├─ membershipRepository.save(membership)
  │    └─ UPDATE user_memberships SET status='CANCELLED', version=? ...
  │
  ├─ auditLogRepository.save(log)
  │    └─ action=CANCELLED
  │
  └─ Returns membership with status=CANCELLED

Response: HTTP 200 OK
```

---

### Flow 6: Scheduled Expiry (Daily Midnight)

```
Spring Scheduler (cron: 0 0 0 * * ?)
  │
  ├─ membershipRepository.findExpiredSubscriptions(LocalDate.now())
  │    └─ SELECT * FROM user_memberships WHERE status='ACTIVE' AND end_date < today
  │
  ├─ For each expired membership:
  │    ├─ membership.setStatus(EXPIRED)
  │    ├─ activeUserMembershipRepository.deleteByUserId(userId)
  │    ├─ membershipRepository.save(membership)
  │    └─ auditLogRepository.save(log) action=EXPIRED
  │
  └─ [Production extension]:
       If autoRenew=true:
         → Call PaymentService.charge(userId, plan.price)
         → On success: extend endDate + status=ACTIVE
         → On failure: status=EXPIRED + notify user
```

---

## 🚀 Running the Project

### Prerequisites
- Java 17+
- Maven 3.6+ (or use IntelliJ bundled Maven)

### Build
```bash
mvn clean package -DskipTests
```

### Run
```bash
java -jar target/membership-1.0.0.jar
```

### Access
| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/h2-console` | DB browser (JDBC: `jdbc:h2:mem:firstclubdb`) |
| `http://localhost:8080/api/v1/membership/plans` | Quick API test |

---

## 🎬 Demo Walkthrough

```bash
# 1. View all plans and tiers
GET http://localhost:8080/api/v1/membership/plans

# 2. Subscribe user1 to MONTHLY plan (auto-assigns SILVER tier)
POST http://localhost:8080/api/v1/membership/subscribe
{ "userId": "user1", "planType": "MONTHLY" }

# 3. Manually upgrade tier
PUT http://localhost:8080/api/v1/membership/upgrade
{ "userId": "user1", "targetTier": "GOLD", "reason": "Promo upgrade" }

# 4. Simulate order activity → auto-evaluate tier
POST http://localhost:8080/api/v1/membership/evaluate-tier
{ "userId": "user1", "orderCount": 25, "totalOrderValue": 25000, "userCohort": "STANDARD" }
# → Auto-promoted to PLATINUM (orderCount 25 >= 20)

# 5. Check current status
GET http://localhost:8080/api/v1/membership/status/user1

# 6. View full audit history
GET http://localhost:8080/api/v1/membership/history/user1
# → [SUBSCRIBED → UPGRADED → TIER_AUTO_UPGRADED]

# 7. Cancel membership
PUT http://localhost:8080/api/v1/membership/cancel/user1
```

---

## 📌 Key Design Decisions Summary

| Decision | Why |
|----------|-----|
| Benefits stored in DB, not code | Product team can change discounts without engineering deployment |
| Criteria stored in DB, not code | Thresholds can be adjusted without code change |
| OR semantics for tier criteria | More forgiving — high spenders qualify even with few orders |
| Pessimistic lock on all writes | Prevents concurrent duplicate subscriptions and conflicting tier changes |
| Optimistic lock (`@Version`) | Last-resort defense against concurrent updates via different code paths |
| Shadow table (`ActiveUserMembership`) | PK constraint prevents duplicate ACTIVE memberships even across instances |
| Partial unique index via `data.sql` | Database-level guarantee of one-active-per-user; JPA `@Index` lacks WHERE support |
| Composite indexes over standalone | `(userId, status)` and `(status, endDate)` prevent full-table scans on low-cardinality columns |
| ShedLock for `@Scheduled` jobs | Ensures scheduled tasks run on exactly one instance in a multi-node deployment |
| `userId` as String (not FK) | Keeps Membership Service decoupled from User Service — true microservice |
| `startDate`/`endDate` not reset on tier change | Tier is just a benefit level — it should not affect the billing period |
| Strategy + Factory pattern | Adding new criteria type requires zero changes to existing classes |
| Audit log as append-only entity | Immutable history — cannot be tampered with after the fact |
| `@Scheduled` expiry processor | Passive expiry check — avoids relying on clients to detect expiry |


