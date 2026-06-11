package com.firstclub.membership.entity;

import com.firstclub.membership.enums.TierLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a membership tier (Silver, Gold, Platinum).
 * Each tier has configurable benefits and eligibility criteria.
 *
 * Tiers are ordered by tierOrder: Silver(1) < Gold(2) < Platinum(3).
 * This ordering is used for upgrade/downgrade logic.
 */
@Entity
@Table(name = "membership_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private TierLevel tierLevel;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Used for ordering tiers; higher = more privileged.
     */
    @Column(nullable = false)
    private Integer tierOrder;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Configurable benefits for this tier (e.g., FREE_DELIVERY=true, DISCOUNT_PERCENT=10).
     * Stored as separate entities so they can be modified without a code deployment.
     */
    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<TierBenefit> benefits = new ArrayList<>();

    /**
     * Eligibility criteria for auto-promotion to this tier.
     * Multiple criteria can be defined; any one being satisfied qualifies the user.
     */
    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<TierCriteria> criteria = new ArrayList<>();
}
