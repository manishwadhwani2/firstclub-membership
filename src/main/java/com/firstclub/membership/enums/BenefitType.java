package com.firstclub.membership.enums;

/**
 * Benefit keys that can be configured per tier.
 * Stored as TierBenefit entities so they are configurable without code changes.
 */
public enum BenefitType {
    FREE_DELIVERY,
    DISCOUNT_PERCENT,
    EXCLUSIVE_DEALS,
    EARLY_ACCESS_SALES,
    PRIORITY_SUPPORT,
    EXCLUSIVE_COUPONS,
    FASTER_DELIVERY_HOURS
}
