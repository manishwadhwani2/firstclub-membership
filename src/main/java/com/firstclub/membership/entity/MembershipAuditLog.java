package com.firstclub.membership.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit log for all membership state changes.
 * Provides a complete history trail for debugging, compliance, and analytics.
 */
@Entity
@Table(name = "membership_audit_logs",
        indexes = @Index(name = "idx_audit_userId", columnList = "userId"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    /**
     * Action performed: SUBSCRIBED, UPGRADED, DOWNGRADED, CANCELLED, TIER_EVALUATED, RENEWED
     */
    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String previousState;

    @Column(columnDefinition = "TEXT")
    private String newState;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
