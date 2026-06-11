# ðŸ›’ FirstClub Membership Program â€” Backend System

> A production-grade membership subscription service with tiered benefits, configurable criteria, and concurrency-safe operations built with Java 17 + Spring Boot 3.2.

---

## ðŸ“‹ Table of Contents

- [Problem Statement](#-problem-statement)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Entity Design & Rationale](#-entity-design--rationale)
- [Design Patterns Used](#-design-patterns-used)
- [Concurrency Handling](#-concurrency-handling)
- [API Reference](#-api-reference)
- [Request Flow â€” End to End](#-request-flow--end-to-end)
- [Extensibility Guide](#-extensibility-guide)
- [Running the Project](#-running-the-project)
- [Demo Walkthrough](#-demo-walkthrough)

---

## ðŸŽ¯ Problem Statement

Design a backend system for a **Membership Program** for FirstClub that supports:

- Monthly, Quarterly, and Yearly subscription plans
- Tiered membership (Silver, Gold, Platinum) with configurable benefits
- Automatic tier progression based on order activity (order count, order value, cohort)
- User actions: subscribe, upgrade tier, downgrade tier, cancel, track membership
- Concurrency-safe operations
- Extensible and modular design

---

## ðŸ›  Tech Stack

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

## ðŸ— Architecture Overview

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                        Client / API Consumer                 â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚ HTTP Request
                          â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                   MembershipController                       â”‚
â”‚        (REST layer â€” validates input, routes to service)     â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚
          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
          â–¼                                â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”            â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ MembershipServiceâ”‚            â”‚ TierEvaluationServiceâ”‚
â”‚ (business logic) â”‚            â”‚ (strategy evaluation)â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜            â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â”‚                                 â”‚
         â”‚                    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
         â”‚                    â”‚  TierCriteriaStrategyFactoryâ”‚
         â”‚                    â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚
         â”‚                    â”‚  â”‚OrderCountStrategy    â”‚  â”‚
         â”‚                    â”‚  â”‚OrderValueStrategy    â”‚  â”‚
         â”‚                    â”‚  â”‚CohortStrategy        â”‚  â”‚
         â”‚                    â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚
         â”‚                    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                  Repository Layer                  â”‚
â”‚  MembershipPlanRepo  â”‚  MembershipTierRepo        â”‚
â”‚  UserMembershipRepo  â”‚  MembershipAuditLogRepo    â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚               H2 / PostgreSQL Database             â”‚
â”‚  membership_plans  â”‚  membership_tiers            â”‚
â”‚  tier_benefits     â”‚  tier_criteria               â”‚
â”‚  user_memberships  â”‚  membership_audit_logs       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## ðŸ“ Entity Design & Rationale

### Why these 7 entities?

The core design decision was to **separate concerns at the data level**. Benefits and criteria are NOT hardcoded â€” they are stored as entities so they can be changed without a code deployment.

---

### `MembershipPlan`
```
id | planType (MONTHLY/QUARTERLY/YEARLY) | name | price | durationDays | description | active
```

**Why:** Plans define the *billing cycle and price*. They are intentionally decoupled from tiers â€” a user can be on a MONTHLY plan with a PLATINUM tier. Plan controls *when* you pay; Tier controls *what* you get.

**Decision:** `planType` is `UNIQUE` in DB â€” prevents accidental duplicate plans.

---

### `MembershipTier`
```
id | tierLevel (SILVER/GOLD/PLATINUM) | name | description | tierOrder | active
```

**Why:** `tierOrder` (1, 2, 3) is the key field that enables upgrade/downgrade validation without string comparisons. The `TierLevel` enum exposes `isHigherThan()` and `isLowerThan()` methods that use `tierOrder` internally.

**Decision:** Tier and Plan are separate entities. This allows future scenarios like:
- *"Only YEARLY plan users can access PLATINUM tier"* â€” add a `planEligibility` field to `MembershipTier`
- *"Free tier with no plan"* â€” just create a MONTHLY plan with price=0

---

### `TierBenefit`
```
id | tier_id (FK) | benefitType (enum) | benefitValue (String) | description
```

**Why:** Benefits are stored as key-value pairs per tier. This means the product team can change *"Gold tier gives 10% discount"* to *"12% discount"* by updating a DB row â€” **no code change, no redeployment**.

**Decision:** `benefitValue` is a `String` intentionally. Different benefit types have different value types:
- `FREE_DELIVERY` â†’ `"true"` / `"false"`
- `DISCOUNT_PERCENT` â†’ `"10"`
- `FASTER_DELIVERY_HOURS` â†’ `"24"`

The consumer (frontend/downstream service) parses the value based on `benefitType` context. This trades type-safety for maximum flexibility.

**Constraint:** `UNIQUE(tier_id, benefit_type)` â€” a tier cannot have duplicate benefit types.

---

### `TierCriteria`
```
id | tier_id (FK) | criteriaType (ORDER_COUNT/ORDER_VALUE/COHORT) | threshold | cohortValue | description
```

**Why:** Eligibility rules for automatic tier promotion are stored in the DB. This means:
- *"Raise GOLD threshold from 5 orders to 10 orders"* â†’ `UPDATE tier_criteria SET threshold=10 WHERE ...`
- No code change needed.

**Decision:** Multiple criteria per tier use **OR semantics** â€” satisfying ANY ONE criteria qualifies the user. This is more practical than AND semantics for retail use cases (a high-value customer who placed 2 large orders should get Gold, even without hitting 5 orders).

**Decision:** `threshold` and `cohortValue` are separate nullable fields. `ORDER_COUNT`/`ORDER_VALUE` use `threshold`; `COHORT` uses `cohortValue`. This avoids a generic `value: String` field that would be harder to query and validate.

---

### `UserMembership`
```
id | userId | plan_id (FK) | tier_id (FK) | status | startDate | endDate | autoRenew | version | createdAt | updatedAt
```

**Why `@Version` field:** This is the **optimistic locking** key. Every time a `UserMembership` row is updated, Hibernate increments `version`. If two threads try to update the same row simultaneously, the second one gets an `OptimisticLockException` â€” preventing silent data corruption.

**Why `userId` is a String (not FK to a User table):** This service is designed as a **microservice**. It doesn't own the User domain. `userId` is an external identifier from the User Service. This keeps the Membership Service independently deployable and testable.

**Decision â€” `startDate`/`endDate` are `LocalDate` not `LocalDateTime`:** Membership validity is day-granular, not minute-granular. Using `LocalDate` avoids timezone issues.

**Decision â€” Composite Indexes:** Strategic composite indexes are used instead of low-cardinality standalone indexes:
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
- **Debugging** â€” *"Why is this user on PLATINUM?"*
- **Compliance** â€” full history of billing-related changes
- **Analytics** â€” how many users upgrade within 30 days of subscribing?

**Decision:** This entity has no setters and no `@PreUpdate` â€” it is **append-only**. Once a log entry is created, it is never modified.

---

## ðŸ§© Design Patterns Used

### 1. Strategy Pattern â€” Tier Criteria Evaluation

**Problem:** We have 3 types of criteria (ORDER_COUNT, ORDER_VALUE, COHORT), each with different evaluation logic. Adding a 4th type (e.g., REFERRAL_COUNT) should not require changing existing code.

**Solution:** Each criteria type maps to a `TierCriteriaStrategy` implementation.

```
TierCriteriaStrategy (interface)
    â”œâ”€â”€ OrderCountCriteriaStrategy  â†’ handles ORDER_COUNT
    â”œâ”€â”€ OrderValueCriteriaStrategy  â†’ handles ORDER_VALUE
    â””â”€â”€ CohortCriteriaStrategy      â†’ handles COHORT
```

**Interface Contract:**
```java
public interface TierCriteriaStrategy {
    CriteriaType getType();
    boolean evaluate(TierCriteria criteria, TierEvaluationContext context);
}
```

**Why:** Open/Closed Principle â€” open for extension (new strategy), closed for modification (no changes to `TierEvaluationService`).

### 2. Factory Pattern â€” Strategy Resolution

**Problem:** Given a `CriteriaType` from the database, how do we get the right strategy at runtime without a giant `if-else` or `switch` block?

**Solution:** `TierCriteriaStrategyFactory` auto-discovers all `TierCriteriaStrategy` beans via Spring DI and builds a `Map<CriteriaType, TierCriteriaStrategy>`.

```java
public TierCriteriaStrategyFactory(List<TierCriteriaStrategy> strategies) {
    this.strategyMap = strategies.stream()
        .collect(Collectors.toMap(TierCriteriaStrategy::getType, Function.identity()));
}
```

**Why:** Zero-configuration factory. Adding a new strategy = just create a new `@Component` class. The factory picks it up automatically on startup.

### 3. Repository Pattern

**Problem:** Business logic should not be coupled to database queries.

**Solution:** Spring Data JPA repositories abstract all data access. The service layer only calls repository methods â€” it has no SQL or JPQL awareness.

**Special addition â€” Pessimistic Locking in Repository:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT um FROM UserMembership um WHERE um.userId = :userId AND um.status = :status")
Optional<UserMembership> findByUserIdAndStatusWithLock(...);
```

**Why:** The repository is the right place to declare locking strategy â€” not the service layer. This keeps the service clean and makes the locking behavior explicitly visible at the data access layer.

### 4. Builder Pattern (via Lombok)

All entities and DTOs use `@Builder`. This gives:
- Immutable-style construction with named parameters
- No telescoping constructors
- Easy to read at call sites: `UserMembership.builder().userId("u1").plan(plan).build()`

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

**Why:** Consistent response envelope across all endpoints. The client always knows where to find data, error messages, and timestamp â€” regardless of which endpoint they call.

---

## ðŸ”’ Concurrency Handling

Two layers of concurrency protection are used, each solving a different problem.

### Layer 1: Pessimistic Locking (DB-level)
**Where:** `UserMembershipRepository.findByUserIdAndStatusWithLock()`

**When used:** Subscribe, upgrade, downgrade, cancel, evaluate-tier â€” any **write** operation.

**How it works:**
```sql
SELECT * FROM user_memberships
WHERE user_id = 'user1' AND status = 'ACTIVE'
FOR UPDATE;  â†  DB row is locked until transaction commits
```

**Why:** Prevents two simultaneous requests (e.g., two upgrade calls) from both reading the same membership state and both writing conflicting updates. The second request waits until the first transaction completes.

### Layer 2: Optimistic Locking (@Version)
**Where:** `UserMembership` entity has a `@Version Long version` field.

**How it works:**
```sql
UPDATE user_memberships
SET tier_id = 2, version = 2     â†  Hibernate increments version
WHERE id = 1 AND version = 1;    â†  Only succeeds if version hasn't changed
```

If `version` was already updated by another thread, Hibernate throws `OptimisticLockException`.

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

### Layer 4: Distributed Scheduler Lock (ShedLock)

**Problem:** `@Scheduled` fires on **every** running instance simultaneously. 

**Solution — ShedLock with JDBC provider:**
```java
@Scheduled(cron = "0 0 0 * * ?")
@SchedulerLock(
    name           = "processExpiredMemberships",
    lockAtLeastFor = "PT1M",
    lockAtMostFor  = "PT10M"
)
public void processExpiredMemberships() { ... }
```

ShedLock uses a `shedlock` database table with a primary key constraint on the lock name. All instances attempt to INSERT simultaneously — only one succeeds, that instance runs the job, the others skip.

---

## ðŸ“¡ API Reference

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

---

## ðŸ”„ Request Flow â€” End to End

### Flow 1: GET /api/v1/membership/plans

```
Client
  â”‚
  â”‚ GET /api/v1/membership/plans
  â–¼
MembershipController.getPlansAndTiers()
  â”‚
  â”‚ No auth/lock needed â€” read-only
  â–¼
MembershipServiceImpl.getPlansAndTiers()
  â”‚
  â”œâ”€ planRepository.findByActiveTrue()
  â”‚    â””â”€ SELECT * FROM membership_plans WHERE active = true
  â”‚
  â”œâ”€ tierRepository.findByActiveTrueOrderByTierOrderAsc()
  â”‚    â””â”€ For each tier â†’ lazily loads tier_benefits + tier_criteria
  â”‚
  â”œâ”€ Maps each plan â†’ PlanResponse (DTO)
  â”œâ”€ Maps each tier â†’ TierResponse (DTO)
  â”‚    â””â”€ benefits â†’ Map<BenefitType, String>
  â”‚    â””â”€ criteria â†’ List<CriteriaResponse>
  â”‚
  â””â”€ Returns PlansAndTiersResponse wrapped in ApiResponse<T>

Response: HTTP 200 with plans[] + tiers[] + benefits + criteria
```

---

### Flow 2: POST /api/v1/membership/subscribe

```
Client
  â”‚
  â”‚ POST /subscribe { userId, planType, tierLevel? }
  â–¼
MembershipController.subscribe()
  â”‚
  â”‚ @Valid validates: userId not blank, planType not null
  â–¼
MembershipServiceImpl.subscribe()
  â”‚
  â”œâ”€ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  â”‚    â””â”€ SELECT ... FOR UPDATE
  â”‚    â””â”€ If found â†’ throw MembershipException(409)
  â”‚
  â”œâ”€ activeUserMembershipRepository.save(userId)
  â”‚    â””â”€ PK constraint prevents duplicate ACTIVE across instances
  â”‚
  â”œâ”€ Build UserMembership (startDate=today, endDate=today+plan.durationDays, status=ACTIVE)
  â”œâ”€ membershipRepository.save(membership)
  â”œâ”€ auditLogRepository.save(log) action=SUBSCRIBED
  â”‚
  â””â”€ Returns UserMembershipResponse (plan + tier + benefits + dates)

Response: HTTP 201 Created
```

---

### Flow 3: PUT /api/v1/membership/upgrade

```
Client
  â”‚
  â”‚ PUT /upgrade { userId, targetTier, reason }
  â–¼
MembershipServiceImpl.upgradeTier()
  â”‚
  â”œâ”€ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  â”œâ”€ Validation: targetTier.isHigherThan(currentTier) using tierOrder
  â”œâ”€ membership.setTier(targetTier) (startDate/endDate UNCHANGED)
  â”œâ”€ membershipRepository.save(membership)
  â”œâ”€ auditLogRepository.save(log) action=UPGRADED
  â”‚
  â””â”€ Returns updated UserMembershipResponse

Response: HTTP 200 OK
Note: Billing period (startDate/endDate) is NOT reset
```

---

### Flow 4: POST /api/v1/membership/evaluate-tier

```
Client (triggered by Order Service after each order)
  â”‚
  â”‚ POST /evaluate-tier { userId, orderCount, totalOrderValue, userCohort }
  â–¼
MembershipController.evaluateTier()
  â–¼
MembershipServiceImpl.evaluateAndUpdateTier()
  â”‚
  â”œâ”€ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  â”‚    â””â”€ If not found â†’ throw ResourceNotFoundException(404)
  â”‚
  â”œâ”€ Build TierEvaluationContext...
  â”‚
  â”œâ”€ TierEvaluationService.evaluateBestTier(context)
  â”‚    â”œâ”€ For GOLD (tierOrder=2):
  â”‚    â”‚    evaluate: 7 >= 5 â†’ TRUE âœ…
  â”‚
  â”œâ”€ membership.setTier(GOLD)
  â”œâ”€ membershipRepository.save(membership)
  â”‚    â””â”€ UPDATE user_memberships SET tier_id=2, version=? ...
  â”‚
  â”œâ”€ auditLogRepository.save(log)
```

---

### Flow 5: PUT /api/v1/membership/cancel/{userId}

```
Client
  â”‚
  â”‚ PUT /cancel/user1
  â–¼
MembershipServiceImpl.cancelMembership()
  â”‚
  â”œâ”€ [LOCK] findByUserIdAndStatusWithLock(userId, ACTIVE)
  â”œâ”€ membership.setStatus(CANCELLED)
  â”œâ”€ activeUserMembershipRepository.deleteByUserId(userId)
  â”œâ”€ membershipRepository.save(membership)
  â”œâ”€ auditLogRepository.save(log) action=CANCELLED
  â”‚
  â””â”€ Returns membership with status=CANCELLED

Response: HTTP 200 OK
```

---

### Flow 6: Scheduled Expiry (Daily Midnight)

```
Spring Scheduler (cron: 0 0 0 * * ?)
  â”‚
  â”œâ”€ membershipRepository.findExpiredSubscriptions(LocalDate.now())
  â”‚    â””â”€ SELECT * FROM user_memberships WHERE status='ACTIVE' AND end_date < today
  â”‚
  â”œâ”€ For each expired membership:
  â”‚    â”œâ”€ membership.setStatus(EXPIRED)
  â”‚    â”œâ”€ membershipRepository.save(membership)
  â”‚    â””â”€ auditLogRepository.save(log) action=EXPIRED
  â”‚
  â””â”€ [Production extension]:
       If autoRenew=true:
         â†’ Call PaymentService.charge(userId, plan.price)
         â†’ On success: extend endDate + status=ACTIVE
         â†’ On failure: status=EXPIRED + notify user
```

---

## ðŸ”Œ Extensibility Guide

### Adding a New Criteria Type (e.g., REFERRAL_COUNT)

**Step 1:** Add to `CriteriaType` enum
```java
public enum CriteriaType {
    ORDER_COUNT, ORDER_VALUE, COHORT,
    REFERRAL_COUNT  // â†  add this
}
```

**Step 2:** Create the strategy
```java
@Component
public class ReferralCountCriteriaStrategy implements TierCriteriaStrategy {
    @Override
    public CriteriaType getType() { return CriteriaType.REFERRAL_COUNT; }

    @Override
    public boolean evaluate(TierCriteria criteria, TierEvaluationContext context) {
        return context.getReferralCount() >= criteria.getThreshold();
    }
}
```

**Step 3:** Add `referralCount` to `TierEvaluationContext`
```java
private final int referralCount;
```

**Step 4:** Insert DB row
```sql
INSERT INTO tier_criteria (criteria_type, threshold, tier_id)
VALUES ('REFERRAL_COUNT', 3, 2);  -- GOLD requires 3 referrals
```

**That's it.** No changes to `TierEvaluationService`, `MembershipServiceImpl`, or any existing code.

---

### Adding a New Benefit Type

**Step 1:** Add to `BenefitType` enum
```java
CASHBACK_PERCENT
```

**Step 2:** Insert DB row
```sql
INSERT INTO tier_benefits (benefit_type, benefit_value, tier_id)
VALUES ('CASHBACK_PERCENT', '5', 3);  -- PLATINUM gets 5% cashback
```

**No code changes needed.**

---

### Adding a New Membership Tier (e.g., DIAMOND)

**Step 1:** Add to `TierLevel` enum
```java
DIAMOND("Diamond", 4)
```

**Step 2:** Insert DB rows
```sql
INSERT INTO membership_tiers (tier_level, name, tier_order, active)
VALUES ('DIAMOND', 'Diamond', 4, true);

INSERT INTO tier_benefits (benefit_type, benefit_value, tier_id) VALUES ...
INSERT INTO tier_criteria (criteria_type, threshold, tier_id) VALUES ...
```

**No code changes needed.** The evaluation service automatically picks up the new tier.

---

## ðŸš€ Running the Project

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

## ðŸŽ¬ Demo Walkthrough

```bash
# 1. View all plans and tiers
GET http://localhost:8080/api/v1/membership/plans

# 2. Subscribe user1 to MONTHLY plan (auto-assigns SILVER tier)
POST http://localhost:8080/api/v1/membership/subscribe
{ "userId": "user1", "planType": "MONTHLY" }

# 3. Manually upgrade tier
PUT http://localhost:8080/api/v1/membership/upgrade
{ "userId": "user1", "targetTier": "GOLD", "reason": "Promo upgrade" }

# 4. Simulate order activity â†’ auto-evaluate tier
POST http://localhost:8080/api/v1/membership/evaluate-tier
{ "userId": "user1", "orderCount": 25, "totalOrderValue": 25000, "userCohort": "STANDARD" }
# â†’ Auto-promoted to PLATINUM (orderCount 25 >= 20)

# 5. Check current status
GET http://localhost:8080/api/v1/membership/status/user1

# 6. View full audit history
GET http://localhost:8080/api/v1/membership/history/user1
# â†’ [SUBSCRIBED â†’ UPGRADED â†’ TIER_AUTO_UPGRADED]

# 7. Cancel membership
PUT http://localhost:8080/api/v1/membership/cancel/user1
```

---

## ðŸ“Œ Key Design Decisions Summary

| Decision | Why |
|----------|-----|
| Benefits stored in DB, not code | Product team can change discounts without engineering deployment |
| Criteria stored in DB, not code | Thresholds can be adjusted without code change |
| OR semantics for tier criteria | More forgiving â€” high spenders qualify even with few orders |
| Pessimistic lock on all writes | Prevents concurrent duplicate subscriptions and conflicting tier changes |
| Optimistic lock (`@Version`) | Last-resort defense against concurrent updates via different code paths |
| Shadow table (`ActiveUserMembership`) | PK constraint prevents duplicate ACTIVE memberships even across instances |
| Partial unique index via `data.sql` | Database-level guarantee of one-active-per-user; JPA `@Index` lacks WHERE support |
| Composite indexes over standalone | `(userId, status)` and `(status, endDate)` prevent full-table scans on low-cardinality columns |
| ShedLock for `@Scheduled` jobs | Ensures scheduled tasks run on exactly one instance in a multi-node deployment |
| `userId` as String (not FK) | Keeps Membership Service decoupled from User Service â€” true microservice |
| `startDate`/`endDate` not reset on tier change | Tier is just a benefit level â€” it should not affect the billing period |
| Strategy + Factory pattern | Adding new criteria type requires zero changes to existing classes |
| Audit log as append-only entity | Immutable history â€” cannot be tampered with after the fact |
| `@Scheduled` expiry processor | Passive expiry check â€” avoids relying on clients to detect expiry |

---

## ðŸ”® Production Considerations (Not Yet Implemented)

| Feature | Notes |
|---------|-------|
| **Payment Integration** | Integrate Razorpay/Stripe â€” charge on subscribe, auto-renew, plan upgrade |
| **Plan Upgrade API** | `PUT /upgrade-plan` with proration calculation |
| **Database** | Replace H2 with PostgreSQL/MySQL for production |
| **Migration** | Use Flyway or Liquibase instead of `ddl-auto: create-drop` |
| **Auth** | Add Spring Security + JWT â€” verify `userId` from token, not request body |
| **Kafka Events** | Publish `membership.subscribed`, `membership.tier_changed` events for downstream services |
| **Caching** | Cache `getPlansAndTiers()` response with Redis (changes rarely) |
| **Rate Limiting** | Prevent abuse on subscribe endpoint |
| **Monitoring** | Micrometer + Prometheus + Grafana for membership metrics |

---

*Built for FirstClub â€” Assignment submission for Membership Program Backend System*
