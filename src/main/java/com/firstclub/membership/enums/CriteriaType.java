package com.firstclub.membership.enums;

/**
 * Defines the type of criteria used for automatic tier progression.
 * Each type maps to a specific TierCriteriaStrategy implementation.
 *
 * Extensible: add a new enum value + a new Strategy class to support a new criteria type.
 */
public enum CriteriaType {
    /**
     * Tier upgrade based on number of orders placed by the user.
     */
    ORDER_COUNT,

    /**
     * Tier upgrade based on total value of orders placed in a billing period.
     */
    ORDER_VALUE,

    /**
     * Tier upgrade based on user belonging to a predefined cohort (e.g., PREMIUM, VIP).
     */
    COHORT
}
