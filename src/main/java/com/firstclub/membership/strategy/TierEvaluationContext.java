package com.firstclub.membership.strategy;

import lombok.Builder;
import lombok.Getter;

/**
 * Runtime context passed to TierCriteriaStrategy during evaluation.
 * Contains all data points needed for any criteria type.
 *
 * Extensibility: add new fields here when new criteria types need new data.
 */
@Getter
@Builder
public class TierEvaluationContext {

    /**
     * External user identifier.
     */
    private final String userId;

    /**
     * Total number of orders placed by this user (all-time or within a period).
     */
    private final int orderCount;

    /**
     * Total value of orders placed by this user in the current billing period.
     */
    private final double totalOrderValue;

    /**
     * User's cohort identifier (e.g., "PREMIUM", "VIP", "STANDARD").
     * Null if user has no special cohort.
     */
    private final String userCohort;
}
