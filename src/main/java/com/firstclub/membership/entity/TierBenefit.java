package com.firstclub.membership.entity;

import com.firstclub.membership.enums.BenefitType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Configurable benefit associated with a MembershipTier.
 *
 * Stored as key-value pairs so tier benefits can be updated without code changes.
 *
 * Examples:
 *   SILVER: FREE_DELIVERY=false, DISCOUNT_PERCENT=5
 *   GOLD:   FREE_DELIVERY=true,  DISCOUNT_PERCENT=10, EXCLUSIVE_DEALS=true
 *   PLATINUM: DISCOUNT_PERCENT=15, PRIORITY_SUPPORT=true
 */
@Entity
@Table(name = "tier_benefits",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tier_id", "benefit_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Column(name = "benefit_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BenefitType benefitType;

    /**
     * Value of the benefit (e.g., "true", "10", "24").
     * Consumers must parse this based on benefitType context.
     */
    @Column(nullable = false)
    private String benefitValue;

    @Column(columnDefinition = "TEXT")
    private String description;
}
