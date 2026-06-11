package com.firstclub.membership.entity;

import com.firstclub.membership.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core entity representing a user's active membership subscription.
 *
 * Concurrency Handling:
 * - @Version enables Optimistic Locking: concurrent updates cause OptimisticLockException
 *   rather than silent data corruption.
 * - The repository layer adds @Lock(PESSIMISTIC_WRITE) for subscribe and tier-change
 *   operations to prevent two threads from modifying the same user's membership simultaneously.
 */
@Entity
@Table(name = "user_memberships",
        indexes = {
                /**
                 * FIX: Composite index replaces two separate indexes.
                 *
                 * Old (problematic):
                 *   @Index(columnList = "userId")   ← useful alone
                 *   @Index(columnList = "status")   ← low-cardinality (4 values), often ignored by optimizer
                 *
                 * New (correct):
                 *   Composite (userId, status) → covers the most frequent query:
                 *   "WHERE user_id = ? AND status = 'ACTIVE'"
                 *   in a single index scan with no extra filtering step.
                 *
                 * Note: The UNIQUE partial index "WHERE status='ACTIVE'" is created
                 * separately in data.sql — @Index annotation cannot express a WHERE clause.
                 */
                @Index(name = "idx_user_status_composite", columnList = "userId, status"),

                /**
                 * Covers the scheduler query:
                 * "WHERE status = 'ACTIVE' AND end_date < today"
                 * Used by processExpiredMemberships() daily job.
                 */
                @Index(name = "idx_status_endDate", columnList = "status, endDate")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External user identifier. Not a FK to a User table to keep this service decoupled.
     */
    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean autoRenew = true;

    /**
     * Optimistic locking version field.
     * Prevents lost-update anomalies in concurrent modification scenarios.
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(this.endDate);
    }

    public boolean isActive() {
        return MembershipStatus.ACTIVE.equals(this.status) && !isExpired();
    }
}
