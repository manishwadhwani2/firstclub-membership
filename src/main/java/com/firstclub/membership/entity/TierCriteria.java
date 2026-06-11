package com.firstclub.membership.entity;

import com.firstclub.membership.enums.CriteriaType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Configurable eligibility criteria for auto-promotion to a MembershipTier.
 *
 * Multiple criteria can be associated with a tier.
 * ANY satisfied criteria qualifies the user for that tier (OR semantics).
 *
 * Examples:
 *   GOLD:     ORDER_COUNT >= 5      OR  ORDER_VALUE >= 5000
 *   PLATINUM: ORDER_COUNT >= 20     OR  ORDER_VALUE >= 20000  OR  COHORT = PREMIUM
 *
 * Extensibility: add a new CriteriaType enum value + a new TierCriteriaStrategy implementation.
 * No changes to this entity or the evaluation service are needed.
 */
@Entity
@Table(name = "tier_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Column(name = "criteria_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CriteriaType criteriaType;

    /**
     * Numeric threshold for ORDER_COUNT and ORDER_VALUE criteria.
     * For COHORT criteria, this is ignored and cohortValue is used.
     */
    @Column
    private Double threshold;

    /**
     * Cohort identifier for COHORT type criteria (e.g., "PREMIUM", "VIP").
     */
    @Column
    private String cohortValue;

    @Column(columnDefinition = "TEXT")
    private String description;
}
